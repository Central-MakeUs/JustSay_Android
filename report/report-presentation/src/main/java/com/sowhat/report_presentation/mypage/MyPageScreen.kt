package com.sowhat.report_presentation.mypage

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.practice.database.entity.MyFeedEntity
import com.sowhat.designsystem.common.Mood
import com.sowhat.designsystem.common.rememberNestedScrollViewState
import com.sowhat.designsystem.component.AppBarMyPage
import com.sowhat.designsystem.component.VerticalNestedScrollView
import com.sowhat.designsystem.theme.JustSayItTheme
import com.sowhat.report_presentation.common.MyFeedEvent
import com.sowhat.report_presentation.common.MyFeedUiState
import com.sowhat.report_presentation.common.toDate
import com.sowhat.report_presentation.component.MyFeed
import com.sowhat.report_presentation.component.RailBackground
import com.sowhat.report_presentation.component.Report

@Composable
fun MyPageRoute(
    navController: NavController,
    viewModel: MyPageViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val myFeedPagingData = viewModel.myFeedPagingData.collectAsLazyPagingItems()
    val myFeedUiState = viewModel.myFeedUiState.collectAsState().value
    
    LaunchedEffect(key1 = myFeedPagingData.loadState) {
        if (myFeedPagingData.loadState.refresh is LoadState.Error) {
            Toast.makeText(context, "error : ${(myFeedPagingData.loadState.refresh as LoadState.Error).error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    MyPageScreen(
        pagingData = myFeedPagingData,
        onEvent = viewModel::onEvent,
        myFeedUiState = myFeedUiState
    )
}

@Composable
fun MyPageScreen(
    modifier: Modifier = Modifier,
    pagingData: LazyPagingItems<MyFeedEntity>,
    onEvent: (MyFeedEvent) -> Unit,
    myFeedUiState: MyFeedUiState
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        val scope = rememberCoroutineScope()
        val nestedScrollViewState = rememberNestedScrollViewState()
        VerticalNestedScrollView(
            modifier = Modifier
                .padding(paddingValues)
                .background(JustSayItTheme.Colors.mainBackground),
            state = nestedScrollViewState,
            header = {
                Report(Modifier,"케이엠", true)
            }
        ) {
            MyFeedItemsScreen(
                modifier = Modifier,
                myFeedUiState = myFeedUiState,
                onEvent = onEvent,
                pagingData = pagingData
            )
        }
    }
}

@Composable
private fun MyFeedItemsScreen(
    modifier: Modifier = Modifier,
    myFeedUiState: MyFeedUiState,
    onEvent: (MyFeedEvent) -> Unit,
    pagingData: LazyPagingItems<MyFeedEntity>
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        val lazyListState = rememberLazyListState()

        val isItemIconVisible = remember {
            derivedStateOf { lazyListState.firstVisibleItemScrollOffset <= 0 }
        }

        val moodItems = Mood.values().toList()
        var currentState by remember { mutableStateOf<Mood?>(null) }
        var currentDate by remember { mutableStateOf<String?>(null) }

        val isScrollInProgress = lazyListState.isScrollInProgress

        AppBarMyPage(
            currentDropdownItem = myFeedUiState.emotion,
            dropdownItems = moodItems,
            isDropdownExpanded = myFeedUiState.isDropdownOpen,
            onDropdownHeaderClick = { isOpen ->
                onEvent(MyFeedEvent.DropdownOpenChanged(isOpen))
            },
            onDropdownMenuChange = { mood ->
                onEvent(MyFeedEvent.EmotionChanged(mood))
            },
            tabItems = myFeedUiState.sortByItems,
            selectedTabItem = myFeedUiState.sortBy,
            selectedTabItemColor = JustSayItTheme.Colors.mainTypo,
            unselectedTabItemColor = JustSayItTheme.Colors.inactiveTypo,
            onSelectedTabItemChange = { tabItem ->
                onEvent(MyFeedEvent.SortChanged(tabItem))
            }
        )

        MyFeedList(
            lazyListState = lazyListState,
            currentState = currentState,
            currentDate = currentDate,
            isScrollInProgress = isScrollInProgress,
            pagingData = pagingData,
            moodItems = moodItems,
            isItemIconVisible = isItemIconVisible,
            onFirstItemIndexChange = { myFeed ->
                currentState = moodItems.find { it.postData == myFeed.writerEmotion }
                currentDate = myFeed.createdAt.toDate()
            }
        )
    }
}

@Composable
private fun MyFeedList(
    lazyListState: LazyListState,
    currentState: Mood?,
    currentDate: String?,
    isScrollInProgress: Boolean,
    pagingData: LazyPagingItems<MyFeedEntity>,
    moodItems: List<Mood>,
    isItemIconVisible: State<Boolean>,
    onFirstItemIndexChange: (MyFeedEntity) -> Unit,
) {
    RailBackground(
        lazyListState = lazyListState,
        currentMood = currentState,
        currentDate = currentDate,
        isScrollInProgress = isScrollInProgress
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = JustSayItTheme.Spacing.spaceSm
                ),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = JustSayItTheme.Spacing.spaceBase)
        ) {
            items(
                count = pagingData.itemCount,
                key = pagingData.itemKey { it.storyId },
                contentType = pagingData.itemContentType { "images" }
            ) { index ->
                val item = pagingData[index]

                item?.let { myFeed ->
                    if (remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }.value == index) {
                        onFirstItemIndexChange(myFeed)
                    }

                    val isFirstItem = remember {
                        derivedStateOf { lazyListState.firstVisibleItemIndex }
                    }.value == index

                    val isMoodVisible = if (isFirstItem) isItemIconVisible.value else true

                    MyFeed(
                        currentDate = currentDate,
                        isPrivate = !myFeed.isOpened,
                        mood = moodItems.find { it.postData == myFeed.writerEmotion },
                        isMoodVisible = isMoodVisible,
                        text = myFeed.bodyText,
                        images = myFeed.photo,
                        onMenuClick = {},
                        date = myFeed.createdAt.toDate(),
                        isScrollInProgress = isScrollInProgress
                    )
                }

                Spacer(modifier = Modifier.height(JustSayItTheme.Spacing.spaceBase))
            }
        }
    }
}

@Preview
@Composable
fun MyPageScreenPreview() {
//    MyPageScreen(
//
//    )
}