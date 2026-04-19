package com.mohamedabdelazeim.zekr.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.data.ZekrData
import com.mohamedabdelazeim.zekr.data.ZekrPrefs
import com.mohamedabdelazeim.zekr.worker.ZekrScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToAdhkar: () -> Unit) {
    val ctx = LocalContext.current
    
    // قراءة الإعدادات
    var enabled by remember { mutableStateOf(ZekrPrefs.isEnabled(ctx)) }
    var selectedInterval by remember { mutableStateOf(ZekrPrefs.getIntervalInMinutes(ctx)) }
    var expanded by remember { mutableStateOf(false) }
    
    // متغيرات جديدة للتحكم في الوضع
    var playbackMode by remember { mutableStateOf(ZekrPrefs.getPlaybackMode(ctx)) } // 0 = ترتيبي، 1 = تكرار
    var selectedRepeatIndex by remember { mutableStateOf(ZekrPrefs.getRepeatIndex(ctx)) }
    var dhikrMenuExpanded by remember { mutableStateOf(false) }

    val intervals = listOf(1,5,10,20,15, 30, 60, 120)
    val gold = Color(0xFFFFD700)
    val darkGreen = Color(0xFF1B5E20)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(R.drawable.bg_father),
            contentDescription = null,            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color(0xEE000000))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            
            // Title
            Text(
                "الحاج محمود عبد العليم كيلانى",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = gold,
                textAlign = TextAlign.Center
            )
            Text(
                "ذكر",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "🤲",
                fontSize = 36.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(Modifier.height(24.dp))

            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1B2E1C))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "⚙️ إعدادات الذكر",
                        color = gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Interval selector
                    Text("⏱ الفترة الزمنية بين الأذكار", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = "كل $selectedInterval دقيقة",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = gold,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            intervals.forEach { min ->
                                DropdownMenuItem(
                                    text = { Text("$min دقيقة") },
                                    onClick = {
                                        selectedInterval = min
                                        ZekrPrefs.setIntervalInMinutes(ctx, min)
                                        if (enabled) ZekrScheduler.schedule(ctx, min.toLong())
                                        expanded = false
                                    }
                                )                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ================== الجزء الجديد: اختيار الوضع ==================
                    Text("🎮 طريقة التشغيل", color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (playbackMode == 0) "ترتيب تلقائي (دور)" else "تكرار ذكر محدد",
                            color = if (playbackMode == 0) Color.LightGray else gold,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = playbackMode == 1,
                            onCheckedChange = { 
                                playbackMode = if (it) 1 else 0
                                ZekrPrefs.setPlaybackMode(ctx, playbackMode)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = gold,
                                checkedTrackColor = darkGreen
                            )
                        )
                    }

                    // لو اختار تكرار، نعرض قائمة الأذكار
                    if (playbackMode == 1) {
                        Spacer(Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = dhikrMenuExpanded,
                            onExpandedChange = { dhikrMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (selectedRepeatIndex < ZekrData.zekrList.size) 
                                            ZekrData.zekrList[selectedRepeatIndex].name 
                                        else "اختر ذكر...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dhikrMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,                                    focusedBorderColor = gold,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = dhikrMenuExpanded,
                                onDismissRequest = { dhikrMenuExpanded = false }
                            ) {
                                ZekrData.zekrList.forEachIndexed { index, zekr ->
                                    DropdownMenuItem(
                                        text = { Text(zekr.name, color = if (index == selectedRepeatIndex) gold else Color.Black) },
                                        onClick = {
                                            selectedRepeatIndex = index
                                            ZekrPrefs.setRepeatIndex(ctx, index)
                                            dhikrMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // =================================================================

                    Spacer(Modifier.height(16.dp))

                    // Enable Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (enabled) "✅ الأذكار مفعلة" else "⛔ الأذكار متوقفة",
                            color = if (enabled) gold else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { v ->
                                enabled = v
                                ZekrPrefs.setEnabled(ctx, v)
                                if (v) ZekrScheduler.schedule(ctx, selectedInterval.toLong())
                                else ZekrScheduler.cancel(ctx)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = gold
                            )
                        )                    }
                    
                    if (enabled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⏰ سيتذكرك بالأذكار كل $selectedInterval دقيقة",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Zekr list Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1B0F))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "📿 الأذكار المبرمجة",
                        color = gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val zekrNames = listOf(
                        "🤲 الصلاة على النبي ﷺ",
                        "📖 آية الأحزاب",
                        "⭐ الحمد لله",
                        "🤲 اللهم لك الحمد",
                        "⭐ لا حول ولا قوة إلا بالله",
                        "💎 سبحان الله وبحمده",
                        "💎 ربِّ اغفر لي وتُبْ عليّ"
                    )
                    zekrNames.forEach { zekr ->
                        Text(
                            zekr,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Adhkar Button
            Button(
                onClick = onNavigateToAdhkar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = darkGreen)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = gold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "أذكار الصباح والمساء",
                    color = gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
