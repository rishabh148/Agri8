@file:JvmName("MainActivityKt")

package com.example.agri8

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agri8.ui.theme.Agri8Theme
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.tensorflow.lite.DataType
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity: ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // This block is a composable context
            MainScreen() // Call the main screen composable
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val navController = rememberNavController() // Create a NavController
        Agri8Theme {
            val grassGreen = Color(0xFF4CAF50)
            val white = Color.White
            Scaffold(
                topBar = {
                    TopNavBar(grassGreen, white, navController) // Pass the NavController
                },
                content = { paddingValues ->
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            // Your existing main content here
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                color = Color(0xFFF5F5F5) // Light neutral background
                            ) {
                                MainContent(grassGreen) // Call your existing content composable
                            }
                        }
                        composable("app") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                color = Color(0xFFF5F5F5)
                            ) {
                                AppScreen()
                            }
                        }
                        composable("library") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                color = Color(0xFFF5F5F5)
                            ) {
                                LibraryScreen()
                            }
                        }
                        composable("news") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                color = Color(0xFFF5F5F5)
                            ) {
                                NewsScreen()
                            }
                        }
                        composable("shop") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                color = Color(0xFFF5F5F5)
                            ) {
                                ShopScreen()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun TopNavBar(grassGreen: Color, white: Color, navController: NavController) {
    Surface(
        color = grassGreen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp) // Adjust padding if needed
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarButton("Home", grassGreen, white) {
                // Logic for Home button
                navController.navigate("main") // Navigate to the home screen
            }
            NavBarButton("App", grassGreen, white) {
                // Logic for App button
                navController.navigate("app") // Navigate to the app screen
            }
            NavBarButton("Library", grassGreen, white) {
                // Logic for Library button
                navController.navigate("library") // Navigate to the library screen
            }
            NavBarButton("News", grassGreen, white) {
                // Logic for News button
                navController.navigate("news") // Navigate to the news screen
            }
            NavBarButton("Shop", grassGreen, white) {
                // Logic for Shop button
                navController.navigate("shop") // Navigate to the shop screen
            }
        }
    }
}


@Composable
fun NavBarButton(
    label: String,
    grassGreen: Color,
    textColor: Color,
    onClick: () -> Unit // Add this parameter
) {
    Button(
        onClick = { onClick() }, // Call the onClick function when the button is clicked
        colors = ButtonDefaults.buttonColors(containerColor = grassGreen),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(text = label, color = textColor)
    }
}

fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
    val fileDescriptor: AssetFileDescriptor = assetManager.openFd("hub_model.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

@Composable
fun MainContent(grassGreen: Color) {
    var text by remember { mutableStateOf("Welcome to Agri8") }
    var imageUrl by remember { mutableStateOf<Uri?>(null) }
    var resultText by remember { mutableStateOf("No prediction yet") }
    var treatmentText by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val modelFile = remember { loadModelFile(context.assets) }
    val interpreter = remember { Interpreter(modelFile) }
    val treatmentMap by remember { mutableStateOf(loadDiseaseTreatments(context)) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            interpreter.close()
        }
    }

    // For selecting an image from gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                imageUrl = it
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
        }
    )

    fun handleButtonClick() {
        coroutineScope.launch {
            if (bitmap != null) {
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, 224, 224, true)
                val inputArray = FloatArray(224 * 224 * 3)
                val pixelData = IntArray(224 * 224)
                resizedBitmap.getPixels(pixelData, 0, 224, 0, 0, 224, 224)

                for (i in pixelData.indices) {
                    val pixel = pixelData[i]
                    inputArray[i * 3] = ((pixel shr 16) and 0xFF) / 255.0f
                    inputArray[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255.0f
                    inputArray[i * 3 + 2] = (pixel and 0xFF) / 255.0f
                }

                val inputTensor =
                    TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
                inputTensor.loadArray(inputArray)

                val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 38), DataType.FLOAT32)
                interpreter.run(inputTensor.buffer, outputTensor.buffer)

                val result = outputTensor.floatArray
                val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1
                val confidence = result[maxIndex] * 100

                val classLabels = loadClassLabels(context)
                val predictedLabel = if (maxIndex in classLabels.indices) {
                    classLabels[maxIndex]
                } else {
                    "Unknown"
                }

                resultText = if (confidence >= 50) {
                    val treatment = treatmentMap[predictedLabel] ?: "No treatment information available."
                    treatmentText = treatment
                    "Predicted Disease:\n\n" +
                            "• Disease: $predictedLabel\n" +
                            "• Confidence: ${String.format("%.2f", confidence)}%\n" +
                            "• Treatment Information: " +
                            "$treatment"
                } else {
                    "Prediction failed. Please try again."
                }
            } else {
                resultText = "No image selected."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your One Stop Solution to Crop Diagnosis and Treatment",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Upload a picture of your plant's leaf, and our system will diagnose potential diseases.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = grassGreen)
        ) {
            Text("Select Image", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { handleButtonClick() },
            colors = ButtonDefaults.buttonColors(containerColor = grassGreen)
        ) {
            Text("Analyze Image", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

fun loadClassLabels(context: android.content.Context): List<String> {
    val labels = mutableListOf<String>()
    context.assets.open("labels.txt").bufferedReader().useLines { lines ->
        lines.forEach { labels.add(it) }
    }
    return labels
}

fun loadDiseaseTreatments(context: android.content.Context): Map<String, String> {
    val treatmentMap = mutableMapOf<String, String>()
    try {
        val inputStream = context.assets.open("disease_treatment.txt")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        bufferedReader.useLines { lines ->
            lines.forEach { line ->
                val parts = line.split('|', limit = 2)
                if (parts.size == 2) {
                    val diseaseName = parts[0].trim()
                    val treatment = parts[1].trim()
                    treatmentMap[diseaseName] = treatment
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return treatmentMap
}

@Composable
fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppScreen() {
    PlaceholderScreen(
        title = "App",
        description = "Feature modules will appear here."
    )
}

@Composable
fun LibraryScreen() {
    PlaceholderScreen(
        title = "Library",
        description = "Browse agronomy guides, tips, and references."
    )
}

@Composable
fun NewsScreen() {
    PlaceholderScreen(
        title = "News",
        description = "Latest agriculture news and updates."
    )
}

@Composable
fun ShopScreen() {
    PlaceholderScreen(
        title = "Shop",
        description = "Discover tools and supplies for your farm."
    )
}

