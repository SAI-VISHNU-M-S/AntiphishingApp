package com.yourcompany.antiphishingappp // Adjust this if your package name is different!

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 📦 Data Models
data class ScanRequest(val url: String)
data class ScanResponse(val url: String, val risk_score: Int, val verdict: String)

interface PhishingApiService {
    @POST("api/v1/scan")
    suspend fun scanUrl(@Body request: ScanRequest): ScanResponse
}

// 🔌 Network Client
object RetrofitClient {
    // 🟢 PASTE YOUR SECURE OASTIFY LINK HERE (Make sure it ends with a / )
    private const val BASE_URL =  "https://antiphishingbackend-2.onrender.com/"

    val apiService: PhishingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhishingApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhishingScannerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PhishingScannerScreen(modifier: Modifier = Modifier) {
    var urlInput by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<ScanResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AI Threat Shield", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Scan URLs for real-time phishing risks", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Enter or paste URL to scan") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (urlInput.isNotBlank()) {
                    isLoading = true
                    errorMessage = ""
                    scanResult = null
                    scope.launch {
                        try {
                            scanResult = RetrofitClient.apiService.scanUrl(ScanRequest(urlInput))
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Analyze Link", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        scanResult?.let { result ->
            // 💡 FIX: Use lowercase check for the verdict string
            val lowerVerdict = result.verdict.lowercase()

            // 💡 FIX: Define colors based on the lowercase match
            val cardColor = when (lowerVerdict) {
                "malicious" -> Color(0xFFFEE2E2)  // Soft Red
                "suspicious" -> Color(0xFFFEF9C3) // Soft Yellow
                else -> Color(0xFFDCFCE7)         // Soft Green
            }

            val textColor = when (lowerVerdict) {
                "malicious" -> Color(0xFF991B1B)  // Dark Red
                "suspicious" -> Color(0xFF854D0E) // Dark Yellow
                else -> Color(0xFF166534)         // Dark Green
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Display verdict as capitalized for professional looks
                    Text("Verdict: ${result.verdict.replaceFirstChar { it.uppercase() }}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Risk Metric Index: ${result.risk_score}/100", fontSize = 14.sp, color = textColor)
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}