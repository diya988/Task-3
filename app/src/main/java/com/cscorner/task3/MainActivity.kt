
package com.cscorner.task3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WeatherNowApp()
            }
        }
    }
}

@Composable
fun WeatherNowApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var loggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var register by remember { mutableStateOf(false) }

    if (loggedIn) {
        WeatherScreen(
            onLogout = {
                auth.signOut()
                loggedIn = false
            }
        )
    } else {
        AuthScreen(
            isRegister = register,
            onSwitch = { register = !register },
            onSuccess = { loggedIn = true }
        )
    }
}

@Composable
fun AuthScreen(
    isRegister: Boolean,
    onSwitch: () -> Unit,
    onSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌦️", fontSize = 52.sp)
        Text("WeatherNow", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (isRegister) "Create your account" else "Welcome back!")

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (minimum 6 characters)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    message = "Please enter a valid email address."
                } else if (password.length < 6) {
                    message = "Password must contain at least 6 characters."
                } else {
                    loading = true
                    message = ""

                    val task = if (isRegister) {
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                    } else {
                        auth.signInWithEmailAndPassword(email.trim(), password)
                    }

                    task.addOnCompleteListener { result ->
                        loading = false
                        if (result.isSuccessful) {
                            onSuccess()
                        } else {
                            message = result.exception?.localizedMessage
                                ?: "Login failed. Please try again."
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isRegister) "Create Account" else "Login")
            }
        }

        TextButton(onClick = onSwitch) {
            Text(
                if (isRegister) "Already have an account? Login"
                else "New user? Create an account"
            )
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

data class WeatherInfo(
    val city: String,
    val temperature: String,
    val feelsLike: String,
    val humidity: String,
    val wind: String,
    val condition: String
)

@Composable
fun WeatherScreen(onLogout: () -> Unit) {
    var city by remember { mutableStateOf("Kolkata") }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadWeather() {
        if (city.isBlank()) {
            message = "Please enter a city name."
            return
        }

        scope.launch {
            loading = true
            message = ""
            try {
                weather = fetchWeather(city.trim())
            } catch (e: Exception) {
                message = e.message ?: "Unable to fetch weather. Check your connection."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadWeather()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("WeatherNow 🌦️", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onLogout) { Text("Logout") }
        }

        Spacer(Modifier.height(24.dp))
        Text("How's the weather today?", fontSize = 18.sp)

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Enter city") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { loadWeather() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search Weather")
        }

        Spacer(Modifier.height(24.dp))

        if (loading) {
            CircularProgressIndicator()
            Text("Fetching weather...")
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { loadWeather() }) { Text("Try Again") }
        }

        weather?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(info.city, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(info.condition, fontSize = 18.sp)
                    Text(
                        "${info.temperature}°C",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Feels like ${info.feelsLike}°C")

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💧 Humidity")
                            Text("${info.humidity}%", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💨 Wind")
                            Text("${info.wind} km/h", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Weather data provided by Open-Meteo", fontSize = 12.sp)
        }
    }
}

suspend fun fetchWeather(city: String): WeatherInfo =
    withContext(Dispatchers.IO) {
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val geoUrl = URL(
            "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encodedCity&count=1&language=en&format=json"
        )

        val geo = getJson(geoUrl)
        val locations = geo.optJSONArray("results")
            ?: throw Exception("City not found. Check the spelling.")

        if (locations.length() == 0) {
            throw Exception("City not found. Try another city.")
        }

        val location = locations.getJSONObject(0)
        val latitude = location.getDouble("latitude")
        val longitude = location.getDouble("longitude")
        val cityName = location.optString("name", city)
        val country = location.optString("country", "")

        val weatherUrl = URL(
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m," +
                    "apparent_temperature,wind_speed_10m,weather_code"
        )

        val response = getJson(weatherUrl)
        val current = response.getJSONObject("current")
        val code = current.getInt("weather_code")

        WeatherInfo(
            city = if (country.isNotBlank()) "$cityName, $country" else cityName,
            temperature = current.getDouble("temperature_2m").toString(),
            feelsLike = current.getDouble("apparent_temperature").toString(),
            humidity = current.getInt("relative_humidity_2m").toString(),
            wind = current.getDouble("wind_speed_10m").toString(),
            condition = weatherDescription(code)
        )
    }

fun getJson(url: URL): JSONObject {
    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val status = connection.responseCode
        val stream = if (status in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (status !in 200..299) {
            throw Exception("Weather service error ($status). Please try again.")
        }

        return JSONObject(body)
    } finally {
        connection.disconnect()
    }
}

fun weatherDescription(code: Int): String =
    when (code) {
        0 -> "Clear sky ☀️"
        1, 2 -> "Mostly clear 🌤️"
        3 -> "Cloudy ☁️"
        45, 48 -> "Foggy 🌫️"
        51, 53, 55 -> "Drizzle 🌦️"
        61, 63, 65 -> "Rain 🌧️"
        71, 73, 75 -> "Snow ❄️"
        80, 81, 82 -> "Rain showers 🌧️"
        95, 96, 99 -> "Thunderstorm ⛈️"
        else -> "Mixed weather"
    }


