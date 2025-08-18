package com.MohammadNoorAbuAsbe.Infodemy.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.StudentCardRepository
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.StudentCardViewModel
import com.MohammadNoorAbuAsbe.Infodemy.viewmodels.StudentCardViewModelFactory
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCardScreen(navController: NavController) {
    val context = LocalContext.current
    val client = OkHttpClient()
    val tokenManager = TokenManager(context)
    val repository = StudentCardRepository(client)

    val viewModel: StudentCardViewModel = viewModel(
        factory = StudentCardViewModelFactory(repository, tokenManager)
    )

    val uiState by viewModel.uiState.collectAsState()
    val currentDateTime by viewModel.currentDateTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("כרטיס סטודנט/ית", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStudentCard() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF0D47A1)
                        )
                    )
                )
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                uiState.error != null -> {
                    val errorMessage = uiState.error!!
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "שגיאה בטעינת הכרטיס",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshStudentCard() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1976D2)
                                )
                            ) {
                                Text("נסה שוב")
                            }
                        }
                    }
                }

                uiState.studentCard != null -> {
                    val studentCard = uiState.studentCard!!
                    StudentCardContent(
                        studentCard = studentCard,
                        currentDateTime = currentDateTime,
                        uiState = uiState
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentCardContent(
    studentCard: com.MohammadNoorAbuAsbe.Infodemy.data.models.StudentCard,
    currentDateTime: String,
    uiState: com.MohammadNoorAbuAsbe.Infodemy.viewmodels.StudentCardUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Student Card Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Institute Logo
                InstituteLogo(
                    logoBytes = uiState.instituteLogo,
                    isLoading = uiState.isLogoLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Institute Name
                Text(
                    text = studentCard.institute,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Student Information
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = studentCard.studentName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = studentCard.cardText,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Student Photo
                    StudentPhoto(imageBase64 = studentCard.studentImage)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Barcode
                BarcodeImage(barcodeBase64 = studentCard.barcodeBase64)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Time Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "תאריך ושעה נוכחיים",
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentDateTime,
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StudentPhoto(imageBase64: String) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(imageBase64) {
        try {
            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            bitmap = null
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Student Photo",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } ?: run {
        // Fallback placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("תמונה", color = Color.Gray)
        }
    }
}

@Composable
private fun BarcodeImage(barcodeBase64: String) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(barcodeBase64) {
        try {
            val imageBytes = Base64.decode(barcodeBase64, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            bitmap = null
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Barcode",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentScale = ContentScale.FillWidth
        )
    } ?: run {
        // Fallback placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("ברקוד", color = Color.Gray)
        }
    }
}

@Composable
private fun InstituteLogo(logoBytes: ByteArray?, isLoading: Boolean) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(logoBytes) {
        hasError = false
        if (logoBytes != null) {
            try {
                bitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
            } catch (e: Exception) {
                hasError = true
                bitmap = null
            }
        } else {
            bitmap = null
        }
    }

    Box(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1976D2)
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Institute Logo",
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
            else -> {
                // Fallback placeholder when logo fails to load or is null
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "לוגו מוסד",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
