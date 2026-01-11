package com.example.agri8.data.repository

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import com.example.agri8.domain.model.DiseaseInfo
import com.example.agri8.domain.model.DiseaseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for all ML model operations and disease detection.
 * This is the single source of truth for ML model interactions.
 */
@Singleton
class DiseaseDetectionRepository @Inject constructor(
    private val context: Context
) {
    private var modelFile: MappedByteBuffer? = null
    private var diseaseData: Map<Int, DiseaseInfo>? = null
    
    /**
     * Initialize the model and disease data.
     * This should be called before processing images.
     */
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            loadModelFile()
            loadDiseaseData()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Check if the model is ready for use.
     */
    fun isModelReady(): Boolean {
        return modelFile != null && diseaseData != null
    }
    
    /**
     * Load the TensorFlow Lite model from assets.
     */
    private suspend fun loadModelFile(): MappedByteBuffer = withContext(Dispatchers.IO) {
        if (modelFile == null) {
            val fileDescriptor: AssetFileDescriptor = context.assets.openFd("hub_model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            modelFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
        modelFile!!
    }
    
    /**
     * Load disease data from JSON file in assets.
     */
    private suspend fun loadDiseaseData(): Map<Int, DiseaseInfo> = withContext(Dispatchers.IO) {
        if (diseaseData == null) {
            val diseaseMap = mutableMapOf<Int, DiseaseInfo>()
            try {
                val reader = BufferedReader(
                    InputStreamReader(context.assets.open("class_indices.json"))
                )
                val jsonString = reader.readText()
                val jsonObject = JSONObject(jsonString)
                
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
                throw e
            }
            diseaseData = diseaseMap
        }
        diseaseData!!
    }
    
    /**
     * Process an image bitmap and return disease detection result.
     * This method handles all ML model inference operations.
     * 
     * @param bitmap The image bitmap to analyze
     * @return DiseaseResult containing disease name, confidence, and treatment
     * @throws IllegalStateException if model is not initialized
     */
    suspend fun processImage(bitmap: Bitmap): DiseaseResult = withContext(Dispatchers.Default) {
        // Ensure model is loaded
        val model = loadModelFile()
        val data = loadDiseaseData()
        
        val interpreter = Interpreter(model)
        
        try {
            // Preprocess image: resize to 224x224
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            
            // Convert bitmap to float array (normalized to 0-1)
            val inputArray = FloatArray(224 * 224 * 3)
            val pixelData = IntArray(224 * 224)
            resizedBitmap.getPixels(pixelData, 0, 224, 0, 0, 224, 224)
            
            for (i in pixelData.indices) {
                val pixel = pixelData[i]
                // Extract RGB values and normalize to [0, 1]
                inputArray[i * 3] = ((pixel shr 16) and 0xFF) / 255.0f
                inputArray[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255.0f
                inputArray[i * 3 + 2] = (pixel and 0xFF) / 255.0f
            }
            
            // Create input tensor
            val inputTensor = TensorBuffer.createFixedSize(
                intArrayOf(1, 224, 224, 3),
                DataType.FLOAT32
            )
            inputTensor.loadArray(inputArray)
            
            // Create output tensor
            val outputTensor = TensorBuffer.createFixedSize(
                intArrayOf(1, 38),
                DataType.FLOAT32
            )
            
            // Run inference
            interpreter.run(inputTensor.buffer, outputTensor.buffer)
            
            // Get predictions
            val confidences = outputTensor.floatArray
            val maxConfidenceIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
            val confidence = confidences[maxConfidenceIndex]
            
            // Get disease information
            val diseaseInfo = data[maxConfidenceIndex]
                ?: DiseaseInfo("Unknown", "No treatment available")
            
            DiseaseResult(
                diseaseName = diseaseInfo.name,
                confidence = confidence,
                treatment = diseaseInfo.treatment
            )
        } finally {
            interpreter.close()
        }
    }
}
