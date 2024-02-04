package com.practice.feed_data.repository

import android.util.Log
import androidx.room.withTransaction
import com.practice.database.FeedDatabase
import com.practice.database.entity.EntireFeedEntity
import com.practice.feed_data.model.FeedResponse
import com.practice.feed_data.remote.FeedApi
import com.sowhat.common.constants.MOOD_ANGRY
import com.sowhat.common.constants.MOOD_HAPPY
import com.sowhat.common.constants.MOOD_SAD
import com.sowhat.common.constants.MOOD_SURPRISED
import com.sowhat.common.model.Resource
import com.sowhat.feed_domain.model.EntireFeed
import com.sowhat.feed_domain.repository.EntireFeedRepository
import com.sowhat.network.util.getHttpErrorResource
import com.sowhat.network.util.getIOErrorResource
import retrofit2.HttpException
import java.io.IOException

class EntireFeedRepositoryImpl(
    private val feedApi: FeedApi,
    private val feedDatabase: FeedDatabase
) : EntireFeedRepository {
    override suspend fun getEntireFeedList(
        accessToken: String,
        sortBy: String,
        emotionCode: String?,
        lastId: Long?,
        size: Int
    ): Resource<EntireFeed> {
        val entireFeedDto = feedApi.getFeedData(
            accessToken = accessToken,
            sortBy = sortBy,
            emotionCode = emotionCode,
            lastId = lastId,
            size = size
        )

        return entireFeedDto.data?.let {
            Resource.Success(
                code = entireFeedDto.code,
                message = entireFeedDto.message,
                data = EntireFeed(
                    hasNext = it.hasNext,
                    feedItems = getFeedList(it)
                )
            )
        } ?: Resource.Error(
            code = entireFeedDto.code,
            message = entireFeedDto.message,
            data = null
        )
    }

    override suspend fun reportFeed(
        accessToken: String,
        feedId: Long,
        reportCode: String
    ): Resource<Unit?> = try {

        val reportResult = feedApi.reportFeed(
            accessToken = accessToken,
            feedId = feedId,
            reportCode = reportCode
        )

        if (reportResult.isSuccess) {
            Resource.Success(
                data = reportResult.data,
                code = reportResult.code,
                message = reportResult.message
            )
        } else Resource.Error(
            code = reportResult.code,
            message = reportResult.message
        )
    } catch (e: HttpException) {
        getHttpErrorResource(e)
    } catch (e: IOException) {
        getIOErrorResource(e)
    }

    override suspend fun blockUser(
        accessToken: String,
        blockedId: Long
    ): Resource<Unit?> = try {
        val blockResult = feedApi.blockUser(
            accessToken = accessToken,
            blockedId = blockedId
        )
        val dao = feedDatabase.entireFeedDao

        if (blockResult.isSuccess) {
            dao.deleteFeedItemByUserId(blockedId)

            Resource.Success(
                data = blockResult.data,
                code = blockResult.code,
                message = blockResult.message
            )
        } else Resource.Error(
            code = blockResult.code,
            message = blockResult.message
        )
    } catch (e: HttpException) {
        getHttpErrorResource(e)
    } catch (e: IOException) {
        getIOErrorResource(e)
    }

    override suspend fun postFeedEmpathy(
        accessToken: String,
        feedId: Long,
        emotionCode: String
    ): Resource<Unit?> = try {
        val dao = feedDatabase.entireFeedDao

        Log.i("FeedRepository", "emotionCode: $emotionCode")

        val postResult = feedApi.postFeedEmpathy(
            accessToken = accessToken,
            feedId = feedId,
            emotionCode = emotionCode
        )

        if (postResult.isSuccess) {
            feedDatabase.withTransaction {
                val feedItem = dao.getFeedItemByFeedId(feedId)
                when (emotionCode) {
                    MOOD_HAPPY -> {
                        Log.i("FeedRepository", "postFeedEmpathy: happy")
                        val count = (feedItem.happinessCount ?: 0) + 1
                        Log.i("FeedRepository", "feedEmpathyCount: happy : $count")
                        dao.updateFeedItem(feedItem.copy(
                            selectedEmotionCode = emotionCode,
                            happinessCount = count
                        ))
                    }
                    MOOD_SAD -> {
                        val count = (feedItem.sadnessCount ?: 0) + 1
                        dao.updateFeedItem(feedItem.copy(
                            selectedEmotionCode = emotionCode,
                            sadnessCount = count
                        ))
                    }
                    MOOD_ANGRY -> {
                        val count = (feedItem.angryCount ?: 0) + 1
                        dao.updateFeedItem(feedItem.copy(
                            selectedEmotionCode = emotionCode,
                            angryCount = count
                        ))
                    }
                    MOOD_SURPRISED -> {
                        val count = (feedItem.surprisedCount ?: 0) + 1
                        dao.updateFeedItem(feedItem.copy(
                            selectedEmotionCode = emotionCode,
                            surprisedCount = count
                        ))
                    }
                    else -> return@withTransaction
                }

                Log.i("FeedRepository", "postFeedEmpathy: ${dao.getFeedItemByFeedId(feedId)}")
            }

            Resource.Success(
                data = postResult.data,
                code = postResult.code,
                message = postResult.message
            )
        } else Resource.Error(
            code = postResult.code,
            message = postResult.message
        )
    } catch (e: HttpException) {
        getHttpErrorResource(e)
    } catch (e: IOException) {
        getIOErrorResource(e)
    }

    override suspend fun cancelFeedEmpathy(
        accessToken: String,
        feedId: Long,
        previousEmpathy: String?
    ): Resource<Unit?> = try {
        val dao = feedDatabase.entireFeedDao
        val postResult = feedApi.cancelFeedEmpathy(
            accessToken = accessToken,
            feedId = feedId,
        )

        if (postResult.isSuccess) {
            feedDatabase.withTransaction {
                val feedItem = dao.getFeedItemByFeedId(feedId)
                previousEmpathy?.let {
                    when (it) {
                        MOOD_HAPPY -> {
                            Log.i("FeedRepository", "postFeedEmpathy: happy")
                            val count = (feedItem.happinessCount ?: 0) - 1
                            Log.i("FeedRepository", "feedEmpathyCount: happy : $count")
                            dao.updateFeedItem(feedItem.copy(
                                selectedEmotionCode = null,
                                happinessCount = count
                            ))
                        }
                        MOOD_SAD -> {
                            val count = (feedItem.sadnessCount ?: 0) - 1
                            dao.updateFeedItem(feedItem.copy(
                                selectedEmotionCode = null,
                                sadnessCount = count
                            ))
                        }
                        MOOD_ANGRY -> {
                            val count = (feedItem.angryCount ?: 0) - 1
                            dao.updateFeedItem(feedItem.copy(
                                selectedEmotionCode = null,
                                angryCount = count
                            ))
                        }
                        MOOD_SURPRISED -> {
                            val count = (feedItem.surprisedCount ?: 0) - 1
                            dao.updateFeedItem(feedItem.copy(
                                selectedEmotionCode = null,
                                surprisedCount = count
                            ))
                        }
                        else -> return@withTransaction
                    }
                }

                Log.i("FeedRepository", "cancelFeedEmpathy: ${dao.getFeedItemByFeedId(feedId)}")
            }

            Resource.Success(
                data = postResult.data,
                code = postResult.code,
                message = postResult.message
            )
        } else Resource.Error(
            code = postResult.code,
            message = postResult.message
        )
    } catch (e: HttpException) {
        getHttpErrorResource(e)
    } catch (e: IOException) {
        getIOErrorResource(e)
    }

    private fun getFeedList(feedResponse: FeedResponse) =
        feedResponse.storyInfo.map { feed ->
            EntireFeedEntity(
                storyUUID = feed.storyUUID,
                storyId = feed.storyId,
                createdAt = feed.createdAt,
                updatedAt = feed.updatedAt,
                writerId = feed.writerId,
                totalCount = feed.emotionOfEmpathy.totalCount,
                happinessCount = feed.emotionOfEmpathy.happinessCount,
                sadnessCount = feed.emotionOfEmpathy.sadnessCount,
                surprisedCount = feed.emotionOfEmpathy.surprisedCount,
                angryCount = feed.emotionOfEmpathy.angryCount,
                isHappinessSelected = feed.emotionOfEmpathy.isHappinessSelected,
                isSadnessSelected = feed.emotionOfEmpathy.isSadnessSelected,
                isSurprisedSelected = feed.emotionOfEmpathy.isSurprisedSelected,
                isAngrySelected = feed.emotionOfEmpathy.isAngrySelected,
                nickname = feed.profileInfo.nickname,
                profileImg = feed.profileInfo.profileImg,
                bodyText = feed.storyMainContent.bodyText,
                photo = feed.storyMainContent.photo.map { it.photoUrl },
                writerEmotion = feed.storyMainContent.writerEmotion,
                isAnonymous = feed.storyMetaInfo.isAnonymous,
                isModified = feed.storyMetaInfo.isModified,
                isOpened = feed.storyMetaInfo.isOpened,
                selectedEmotionCode = feed.resultOfEmpathize.emotionCode,
                isOwner = feed.isMine
            )
        }
}