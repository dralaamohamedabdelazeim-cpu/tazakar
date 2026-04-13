package com.mohamedabdelazeim.zekr.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedabdelazeim.zekr.data.AdhkarItem
import com.mohamedabdelazeim.zekr.data.ZekrData
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val gold = Color(0xFFFFD700)
    val darkGreen = Color(0xFF1B5E20)

    var tabIndex by remember { mutableStateOf(0) }
    val morningList = remember { ZekrData.loadAdhkar(ctx, true) }
    val eveningList = remember { ZekrData.loadAdhkar(ctx, false) }
    val currentList = if (tabIndex == 0) morningList else eveningList

    val pagerState = rememberPagerState(pageCount = { currentList.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "أذكار الصباح والمساء",
                        color = gold,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B0F))
            )
        },
        containerColor = Color(0xFF0D1B0F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color(0xFF1B2E1C),
                contentColor = gold,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = gold
                    )
                }
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = {
                        tabIndex = 0
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("أذكار الصباح")
                        }
                    },
                    selectedContentColor = gold,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = {
                        tabIndex = 1
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightsStay, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("أذكار المساء")
                        }
                    },
                    selectedContentColor = gold,
                    unselectedContentColor = Color.Gray
                )
            }

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد أذكار", color = Color.Gray)
                }
            } else {
                // Page counter
                Text(
                    "الذكر ${pagerState.currentPage + 1} من ${currentList.size}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val item = currentList[page]
                    AdhkarCard(item = item, gold = gold)
                }

                // Bottom navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage > 0)
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "السابق",
                            tint = if (pagerState.currentPage > 0) gold else Color.Gray
                        )
                    }

                    Text(
                        "${pagerState.currentPage + 1} / ${currentList.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage < currentList.size - 1)
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        enabled = pagerState.currentPage < currentList.size - 1
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "التالي",
                            tint = if (pagerState.currentPage < currentList.size - 1) gold else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdhkarCard(item: AdhkarItem, gold: Color) {
    var count by rememberSaveable(item.id) { mutableStateOf(0) }
    val done = count >= item.repeat

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Zekr Title
        Text(
            item.title,
            color = gold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        // Zekr Text Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2E1C))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.text,
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Tasbeeh Circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    if (done)
                        Brush.radialGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00)))
                    else
                        Brush.radialGradient(listOf(Color(0xFF2E4E30), Color(0xFF1B2E1C)))
                )
                .clickable {
                    if (!done) count++
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(targetState = count, label = "count") { c ->
                    Text(
                        "$c",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (done) Color.Black else Color.White
                    )
                }
                Text(
                    "من ${item.repeat}",
                    color = if (done) Color.Black else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { count = 0 },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                border = BorderStroke(1.dp, gold)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (done) "أتمّ السبحة" else "إعادة العد")
            }
        }

        // Benefit
        if (item.benefit.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2010))
            ) {
                Text(
                    "✨ ${item.benefit}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
