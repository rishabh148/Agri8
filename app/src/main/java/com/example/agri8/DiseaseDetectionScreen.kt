package com.example.agri8

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.content.res.Configuration
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
import com.example.agri8.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale

@Composable
fun DiseaseDetectionScreen(
    onBackToLanguageSelection: () -> Unit = {}
) {
    val context = LocalContext.current
    val grassGreen = Color(0xFF4CAF50)
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var diseaseResult by remember { mutableStateOf<DiseaseResult?>(null) }
    var showBackDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Handle back button press
    BackHandler(enabled = true) {
        if (bitmap != null || diseaseResult != null) {
            // If there's an image or result, clear it first
            bitmap = null
            diseaseResult = null
        } else {
            // If no image/result, show dialog to go back to language selection
            showBackDialog = true
        }
    }
    
    val modelFile = remember { loadModelFile(context.assets) }
    val interpreter = remember { Interpreter(modelFile) }
    val diseaseData = remember { loadDiseaseData(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            interpreter.close()
        }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                diseaseResult = null // Reset previous result
            }
        }
    )
    
    fun analyzeImage() {
        if (bitmap == null) return
        
        isAnalyzing = true
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    processImageWithModel(bitmap!!, interpreter, diseaseData)
                }
                
                // Translate disease name and treatment text to selected language
                val selectedLanguage = LanguageManager.getSelectedLanguage()
                
                val translatedDiseaseName = if (selectedLanguage.isNotEmpty() && selectedLanguage != "en") {
                    try {
                        TranslationHelper.translateText(result.diseaseName, selectedLanguage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        result.diseaseName // Fallback to original if translation fails
                    }
                } else {
                    result.diseaseName
                }
                
                val translatedTreatment = if (selectedLanguage.isNotEmpty() && selectedLanguage != "en") {
                    try {
                        TranslationHelper.translateText(result.treatment, selectedLanguage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        result.treatment // Fallback to original if translation fails
                    }
                } else {
                    result.treatment
                }
                
                diseaseResult = result.copy(
                    diseaseName = translatedDiseaseName,
                    treatment = translatedTreatment
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Show error message - in a real app, you'd show a proper error dialog
                diseaseResult = DiseaseResult(
                    diseaseName = "Error",
                    confidence = 0f,
                    treatment = "Failed to analyze image. Please try again."
                )
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        if (diseaseResult == null) {
            // Centered layout when no result yet (with or without image)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (bitmap == null) {
                    // No image selected - show upload button
                    // Header
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = grassGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.upload_crop_image),
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )
                    
                    // Upload Button - Centered
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = grassGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_image),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Image selected but not analyzed yet - show image and analyze button centered
                    val currentBitmap = bitmap
                    if (currentBitmap != null) {
                        // Header
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = grassGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        // Display Image
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(300.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Analyze Button
                        Button(
                            onClick = { analyzeImage() },
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
                                    text = stringResource(R.string.analyzing),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.analyze_disease),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Scrollable layout when image is selected
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (smaller when image is selected)
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = grassGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Display Image
                bitmap?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Analyze Button
                    Button(
                        onClick = { analyzeImage() },
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
                                text = stringResource(R.string.analyzing),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.analyze_disease),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                // Disease Result
                diseaseResult?.let { result ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DiseaseResultCard(
                        result = result,
                        context = context,
                        grassGreen = grassGreen
                    )
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
                    text = stringResource(R.string.change_language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.go_back_language_selection))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackDialog = false
                        onBackToLanguageSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = grassGreen)
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackDialog = false }
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@Composable
fun DiseaseResultCard(
    result: DiseaseResult,
    context: Context,
    grassGreen: Color
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
            // Disease Name
            Text(
                text = stringResource(R.string.detected_disease),
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
            
            // Confidence
            Text(
                text = "${stringResource(R.string.confidence)}: ${String.format("%.1f", result.confidence * 100)}%",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.LightGray
            )
            
            // Treatment
            Text(
                text = stringResource(R.string.treatment),
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
            
            // YouTube Video Button
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
                    text = stringResource(R.string.watch_treatment_video),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Google Search Button
            Button(
                onClick = {
                    openGoogleSearch(context, result.diseaseName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_google),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

data class DiseaseResult(
    val diseaseName: String,
    val confidence: Float,
    val treatment: String
)

fun loadModelFile(assetManager: android.content.res.AssetManager): MappedByteBuffer {
    val fileDescriptor: AssetFileDescriptor = assetManager.openFd("hub_model.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

fun loadDiseaseData(context: Context): Map<Int, DiseaseInfo> {
    val diseaseMap = mutableMapOf<Int, DiseaseInfo>()
    try {
        val reader = BufferedReader(InputStreamReader(context.assets.open("class_indices.json")))
        val jsonString = reader.readText()
        val jsonObject = org.json.JSONObject(jsonString)
        
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.getJSONObject(key)
            val name = value.getString("name")
            val treatment = value.getString("treatment")
            diseaseMap[key.toInt()] = DiseaseInfo(name, treatment)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return diseaseMap
}

data class DiseaseInfo(
    val name: String,
    val treatment: String
)

suspend fun processImageWithModel(
    bitmap: Bitmap,
    interpreter: Interpreter,
    diseaseData: Map<Int, DiseaseInfo>
): DiseaseResult {
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
    val inputArray = FloatArray(224 * 224 * 3)
    val pixelData = IntArray(224 * 224)
    resizedBitmap.getPixels(pixelData, 0, 224, 0, 0, 224, 224)
    
    for (i in pixelData.indices) {
        val pixel = pixelData[i]
        inputArray[i * 3] = ((pixel shr 16) and 0xFF) / 255.0f
        inputArray[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255.0f
        inputArray[i * 3 + 2] = (pixel and 0xFF) / 255.0f
    }
    
    val inputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
    inputTensor.loadArray(inputArray)
    
    val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 38), DataType.FLOAT32)
    interpreter.run(inputTensor.buffer, outputTensor.buffer)
    
    val confidences = outputTensor.floatArray
    val maxConfidenceIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
    val confidence = confidences[maxConfidenceIndex]
    
    val diseaseInfo = diseaseData[maxConfidenceIndex] ?: DiseaseInfo("Unknown", "No treatment available")
    
    return DiseaseResult(
        diseaseName = diseaseInfo.name,
        confidence = confidence,
        treatment = diseaseInfo.treatment
    )
}

fun openYouTubeVideo(context: Context, diseaseName: String) {
    val searchQuery = "$diseaseName treatment crop disease"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}"))
    context.startActivity(intent)
}

fun openGoogleSearch(context: Context, diseaseName: String) {
    val languageCode = LanguageManager.getSelectedLanguage()
    val searchQuery = "$diseaseName treatment crop disease"
    // Google search URL with language parameter
    val googleUrl = "https://www.google.com/search?q=${Uri.encode(searchQuery)}&hl=$languageCode"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl))
    context.startActivity(intent)
}


