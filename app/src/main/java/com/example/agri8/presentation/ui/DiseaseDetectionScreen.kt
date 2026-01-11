package com.example.agri8.presentation.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.agri8.util.localizedStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.agri8.R
import com.example.agri8.presentation.viewmodel.DiseaseDetectionViewModel

/**
 * Disease Detection Screen - UI Layer
 * This screen only handles UI rendering and user interactions.
 * All business logic is handled by the ViewModel.
 */
@Composable
fun DiseaseDetectionScreen(
    onNavigateToLanguageSelection: () -> Unit = {},
    viewModel: DiseaseDetectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val grassGreen = Color(0xFF4CAF50)
    
    // Observe ViewModel state
    val selectedImage by viewModel.selectedImage.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isModelLoading by viewModel.isModelLoading.collectAsState()
    val diseaseResult by viewModel.diseaseResult.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentLanguageCode by viewModel.currentLanguageCode.collectAsState()
    
    var showBackDialog by remember { mutableStateOf(false) }
    
    // Initialize model after screen transition
    LaunchedEffect(Unit) {
        viewModel.initializeModel()
    }
    
    // Handle back button press
    BackHandler(enabled = true) {
        if (selectedImage != null || diseaseResult != null) {
            viewModel.clearImage()
        } else {
            showBackDialog = true
        }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.setSelectedImage(bitmap)
            }
        }
    )
    
    // Show error message if any
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Error is already displayed in the UI state
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Show loading indicator while model is loading
        if (isModelLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = grassGreen,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = localizedStringResource(R.string.initializing_model),
                        color = Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localizedStringResource(R.string.please_wait),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (diseaseResult == null) {
            // Centered layout when no result yet
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedImage == null) {
                    // No image selected
                    Text(
                        text = localizedStringResource(R.string.app_name),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = grassGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = localizedStringResource(R.string.upload_crop_image),
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )
                    
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = grassGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = localizedStringResource(R.string.select_image),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Image selected but not analyzed yet
                    selectedImage?.let { bitmap ->
                        Text(
                            text = localizedStringResource(R.string.app_name),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = grassGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(300.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = localizedStringResource(R.string.selected_image_desc),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.analyzeImage() },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = grassGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = localizedStringResource(R.string.analyzing),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = localizedStringResource(R.string.analyze_disease),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Scrollable layout when result is available
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = localizedStringResource(R.string.app_name),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = grassGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                selectedImage?.let { bitmap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = localizedStringResource(R.string.selected_image_desc),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.analyzeImage() },
                        enabled = !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = grassGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = localizedStringResource(R.string.analyzing),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = localizedStringResource(R.string.analyze_disease),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                diseaseResult?.let { result ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DiseaseResultCard(
                        result = result,
                        context = context,
                        grassGreen = grassGreen,
                        languageCode = currentLanguageCode
                    )
                }
                
                // Show error message if present
                error?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFC62828),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
    
    // Back to language selection dialog
    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = {
                Text(
                    text = localizedStringResource(R.string.change_language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(localizedStringResource(R.string.go_back_language_selection))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackDialog = false
                        onNavigateToLanguageSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = grassGreen)
                ) {
                    Text(localizedStringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackDialog = false }
                ) {
                    Text(localizedStringResource(R.string.no))
                }
            }
        )
    }
}

@Composable
fun DiseaseResultCard(
    result: com.example.agri8.domain.model.DiseaseResult,
    context: Context,
    grassGreen: Color,
    languageCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = localizedStringResource(R.string.detected_disease),
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = result.diseaseName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = grassGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${localizedStringResource(R.string.confidence)}: ${String.format("%.1f", result.confidence * 100)}%",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.LightGray
            )
            
            Text(
                text = localizedStringResource(R.string.treatment),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = result.treatment,
                fontSize = 16.sp,
                color = Color.Black,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    openYouTubeVideo(context, result.diseaseName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = localizedStringResource(R.string.watch_treatment_video),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    openGoogleSearch(context, result.diseaseName, languageCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = localizedStringResource(R.string.search_google),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

private fun openYouTubeVideo(context: Context, diseaseName: String) {
    val searchQuery = "$diseaseName treatment crop disease"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}"))
    context.startActivity(intent)
}

private fun openGoogleSearch(context: Context, diseaseName: String, languageCode: String) {
    val searchQuery = "$diseaseName treatment crop disease"
    val googleUrl = "https://www.google.com/search?q=${Uri.encode(searchQuery)}&hl=$languageCode"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl))
    context.startActivity(intent)
}
