package com.sowhat.feed_domain

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.practice.database.FeedDatabase
import com.practice.database.entity.EntireFeedEntity
import com.practice.database.entity.MyFeedEntity
import com.sowhat.common.model.Resource
import com.sowhat.datastore.AuthDataRepository
import com.sowhat.feed_domain.model.EntireFeed
import com.sowhat.feed_domain.repository.EntireFeedRepository
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.CancellationException


@OptIn(ExperimentalPagingApi::class)
class EntireFeedRemoteMediator(
    private val entireFeedRepository: EntireFeedRepository,
    private val authDataRepository: AuthDataRepository,
    private val feedDatabase: FeedDatabase,
    private val sortBy: String,
//    private val lastId: Long?,
//    private val hasNext: Boolean,
    private val emotion: String?
) : RemoteMediator<Int, EntireFeedEntity>() {

    private val dao = feedDatabase.entireFeedDao

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EntireFeedEntity>
    ): MediatorResult {
        return try {
            val authData = authDataRepository.authData.first()
            val accessToken = authData.accessToken
            val memberId = authData.memberId

            val loadKey = when (loadType) {
                LoadType.REFRESH -> {
                    // ***중요 : refresh될 때 스크롤 위치 이슈 해결 : 데이터 로드를 위해 로드키를 불러오는 과정에서 모든 데이터를 지워준다
                    feedDatabase.withTransaction {
                        dao.deleteAllFeedItems()
                    }
                    null
                }
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    lastItem?.storyId
                }
            }

            if (accessToken != null && memberId != null) {
                getPagingData(
                    accessToken = accessToken,
                    memberId = memberId,
                    loadKey = loadKey,
                    state = state,
                    loadType = loadType,
                )
            } else MediatorResult.Error(NullPointerException())
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            MediatorResult.Error(e)
        }
    }

    private suspend fun getPagingData(
        accessToken: String,
        memberId: Long,
        loadKey: Long?,
        state: PagingState<Int, EntireFeedEntity>,
        loadType: LoadType,
    ): MediatorResult {
        val feedItems = entireFeedRepository.getEntireFeedList(
            accessToken = accessToken,
            sortBy = sortBy,
            memberId = memberId,
            emotionCode = emotion,
            lastId = loadKey,
            size = state.config.pageSize
        )

        return getPagingDataTransactionResult(feedItems, loadType)
    }

    private suspend fun getPagingDataTransactionResult(
        feedItems: Resource<EntireFeed>,
        loadType: LoadType
    ): MediatorResult {
        return when (feedItems) {
            is Resource.Success -> {
                addFeedItemsToDatabase(loadType, feedItems)

                MediatorResult.Success(
                    endOfPaginationReached = feedItems.data?.hasNext == false
                )
            }

            is Resource.Error -> {
                MediatorResult.Error(NullPointerException())
            }
        }
    }

    private suspend fun addFeedItemsToDatabase(
        loadType: LoadType,
        feedItems: Resource<EntireFeed>
    ) {
        feedDatabase.withTransaction {
            if (loadType == LoadType.REFRESH) {
                dao.deleteAllFeedItems()
            }

            val myFeedEntities = feedItems.data?.feedItems
            myFeedEntities?.let {
                dao.addFeedItems(myFeedEntities)
            }
        }
    }
}