package com.example.agri8

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agri8.ui.theme.Agri8Theme
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


fun provideSarvamApiService(): SarvamApiService {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.sarvam.ai/") // Replace with actual base URL
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(SarvamApiService::class.java)
}


interface SarvamApiService {
    @GET("search") // Replace with the actual endpoint
    fun search(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("api_key") apiKey: String
    ): Call<ApiResponse>
}

// Data class for the response
data class ApiResponse(
    val results: List<Result>
)

data class Result(
    val title: String,
    val description: String
)


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Agri8Theme {
                val grassGreen = Color(0xFF4CAF50) // Grass green background color
                val white = Color.White
                Scaffold(
                    topBar = {
                        Column {
                            // Move the BottomNavBar to the top
                            BottomNavBar(grassGreen, white)
                        }
                    },
                    content = { paddingValues ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            color = Color(0xFFF5F5F5) // Light neutral background
                        ) {
                            MainContent(grassGreen) // Pass grassGreen to MainContent
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(grassGreen: Color, white: Color) {
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
            NavBarButton("Home", grassGreen, white)
            NavBarButton("App", grassGreen, white)
            NavBarButton("Library", grassGreen, white)
            NavBarButton("News", grassGreen, white)
            NavBarButton("Shop", grassGreen, white)
        }
    }
}

@Composable
fun NavBarButton(label: String, grassGreen: Color, textColor: Color) {
    Button(
        onClick = { /*TODO: Navigate*/ },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(grassGreen: Color) {
    var text by remember { mutableStateOf("Welcome to Agri8") }
    var imageUrl by remember { mutableStateOf<Uri?>(null) }
    var resultText by remember { mutableStateOf("No prediction yet") }
    var treatmentText by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedLanguage by remember { mutableStateOf("en") } // Default language

    val context = LocalContext.current
    val modelFile = remember { loadModelFile(context.assets) }
    val interpreter = remember { Interpreter(modelFile) }
    val treatmentMap by remember { mutableStateOf(loadDiseaseTreatments(context)) }
    val coroutineScope = rememberCoroutineScope()

    // Sarvam API service
    val apiService = remember { provideSarvamApiService() }

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

                val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 10), DataType.FLOAT32)
                interpreter.run(inputTensor.buffer, outputTensor.buffer)

                val confidences = outputTensor.floatArray
                val maxConfidenceIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1

                val predictedDisease = treatmentMap.keys.toList()[maxConfidenceIndex]
                resultText = "Predicted Disease: $predictedDisease"
                treatmentText = treatmentMap[predictedDisease] ?: "Treatment not found"

                // Sarvam API integration after prediction
                val apiKey = "YOUR_API_KEY" // Replace with your Sarvam API key
                val searchQuery = predictedDisease
                val call = apiService.search(searchQuery, selectedLanguage, apiKey)
                val response = call.execute()

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val apiResults = apiResponse?.results?.joinToString("\n") { it.description }
                    treatmentText += "\n\nAPI Results:\n$apiResults"
                } else {
                    treatmentText += "\n\nFailed to retrieve additional information."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown for language selection
        var expanded by remember { mutableStateOf(false) }
        val availableLanguages = listOf("en", "hi", "fr", "es", "de")

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedLanguage,
                onValueChange = { selectedLanguage = it },
                label = { Text("Select Language") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            selectedLanguage = language
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text(text = "Pick Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the selected image
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier.size(224.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { handleButtonClick() }) {
            Text(text = "Analyze")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = resultText)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = treatmentText)
    }
}

fun loadDiseaseTreatments(context: android.content.Context): Map<String, String> {
    val treatments = mutableMapOf<String, String>()
    val reader = BufferedReader(InputStreamReader(context.assets.open("class_indices.json")))

    val jsonString = reader.readText()
    val jsonObject = org.json.JSONObject(jsonString)

    val keys = jsonObject.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = jsonObject.getString(key)
        treatments[key] = value
    }
    return treatments
}
