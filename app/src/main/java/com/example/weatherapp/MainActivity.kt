package com.example.weatherapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import java.math.RoundingMode
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var callLay : LinearLayout
    private lateinit var zipEntry : EditText
    private lateinit var callErrorTV : TextView
    private lateinit var callDataButton : Button

    private lateinit var cityLay : LinearLayout
    private lateinit var cityTV : TextView
    private lateinit var dateTV : TextView
    private lateinit var weatherLay : LinearLayout
    private lateinit var skyTV : TextView
    private lateinit var temTV : TextView
    private lateinit var lowTemTV : TextView
    private lateinit var highTemTv : TextView
    private lateinit var infoLay : LinearLayout
    private lateinit var riseTimeTV : TextView
    private lateinit var downTimeTV : TextView
    private lateinit var windSpeedTV : TextView
    private lateinit var pressureTV : TextView
    private lateinit var humidityTV : TextView
    private lateinit var refresh : LinearLayout

    private var mode = 0
    private var zipCode = 0
    private var temp = 0.0
    private var highTemp = 0.0
    private var lowTemp = 0.0
    private var tempMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        callLay = findViewById(R.id.callLay)
        zipEntry = findViewById(R.id.zipEntry)
        callErrorTV = findViewById(R.id.callTV)
        callDataButton = findViewById(R.id.callDataButton)

        cityLay = findViewById(R.id.cityLay)
        cityTV = findViewById(R.id.cityTV)
        dateTV = findViewById(R.id.dateTV)
        weatherLay = findViewById(R.id.weatherLay)
        skyTV = findViewById(R.id.skyTV)
        temTV = findViewById(R.id.temTV)
        lowTemTV = findViewById(R.id.lowTemTV)
        highTemTv = findViewById(R.id.highTemTV)
        infoLay = findViewById(R.id.infoLay)
        riseTimeTV = findViewById(R.id.riseTimeTV)
        downTimeTV = findViewById(R.id.downTimeTV)
        windSpeedTV = findViewById(R.id.windSpeedTV)
        pressureTV = findViewById(R.id.pressureTV)
        humidityTV = findViewById(R.id.humidityTV)
        refresh = findViewById(R.id.refresh)

        callDataButton.setOnClickListener{
            zipCode = 0
            callErrorTV.text = null

            if (zipEntry.text.isNotBlank())
                zipCode = zipEntry.text.toString().toInt()
            requestAPI()

            val handler = Handler()
            /*handler.postDelayed({
                //callErrorTV.text = "Error Wrong Zip Code"
                zipEntry.text = null
                                }, 2000)*/
            handler.postDelayed({
                callErrorTV.text = ""
            }, 5000)


            val view: View? = this.currentFocus
            if (view != null) {
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }

            zipEntry.text = null
            tempMode = 0
        }
        refresh.setOnClickListener{
            zipCode = 52801
            requestAPI()
            tempMode = 0
        }
        cityLay.setOnClickListener{
            change()
        }
        temTV.setOnClickListener{
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            if (tempMode == 0) {
                tempMode = 1
                temp = 32+(temp*9/5)
                temTV.text = "${df.format(temp)}°F"
                highTemp = 32+(highTemp*9/5)
                highTemTv.text = "High: ${df.format(highTemp)}°F"
                lowTemp = 32+(lowTemp*9/5)
                lowTemTV.text = "Low: ${df.format(lowTemp)}°F"
            }
            else{
                tempMode = 0
                temp = (temp-32)*5/9
                temTV.text = "${df.format(temp)}°C"
                highTemp = (highTemp-32)*5/9
                highTemTv.text = "High: ${df.format(highTemp)}°C"
                lowTemp = (lowTemp-32)*5/9
                lowTemTV.text = "Low: ${df.format(lowTemp)}°C"
            }
        }
    }

    private suspend fun errorShows(i:Int){
        withContext(Main) {
            when (i) {
                1 -> callErrorTV.text = "Error Wrong Zip Code"
                2 -> callErrorTV.text = "Error on Getting the Data"
                else -> callErrorTV.text = "Unknown Error"
            }
        }
    }

    private fun requestAPI(){
        CoroutineScope(IO).launch {
            val data = async { fetchWeather() }.await()
            if(data.isNotEmpty()){
                setData(data)
            }else{
                errorShows(1)
                Log.d("MAIN", "Unable to get data")
            }
        }
    }

    private fun fetchWeather(): String{
        var error = 0
        var response = ""
        try{
            response = URL("https://api.openweathermap.org/data/2.5/weather?zip=$zipCode&units=metric&appid=05d0f5b73d3d8032629902e2cbb33870").readText(Charsets.UTF_8)
        }catch(e: Exception){
            Log.d("MAIN", "$e")
        }
        return response
    }

    private suspend fun setData(result: String){
       try{
            withContext(Main) {
                val jsonObj = JSONObject(result)

                cityTV.text = "${jsonObj.getString("name")}, ${jsonObj.getJSONObject("sys").getString("country")}"
                val lastUpdateText = "Updated at: " + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(jsonObj.getLong("dt")*1000))
                dateTV.text = lastUpdateText
                val weather = jsonObj.getJSONArray("weather")
                val description = weather.getJSONObject(0).getString("description")
                skyTV.text = description
                temp = jsonObj.getJSONObject("main").getString("temp").toDouble()
                temTV.text = "$temp°C"
                lowTemp = jsonObj.getJSONObject("main").getString("temp_min").toDouble()
                lowTemTV.text = "Low: ${lowTemp}°C"
                highTemp = jsonObj.getJSONObject("main").getString("temp_max").toDouble()
                highTemTv.text = "High: ${highTemp}°C"
                val riseTime = SimpleDateFormat(
                    "hh:mm a",
                    Locale.ENGLISH
                ).format(Date(jsonObj.getJSONObject("sys").getLong("sunrise") * 1000))
                riseTimeTV.text = "$riseTime"
                val downTime = SimpleDateFormat(
                    "hh:mm a",
                    Locale.ENGLISH
                ).format(Date(jsonObj.getJSONObject("sys").getLong("sunset") * 1000))
                downTimeTV.text = "$downTime"
                windSpeedTV.text = jsonObj.getJSONObject("wind").getString("speed")
                pressureTV.text = jsonObj.getJSONObject("main").getString("pressure")
                humidityTV.text = jsonObj.getJSONObject("main").getString("humidity")
                mode = 0
                change()
            }
       }
        catch (e:Exception){
            errorShows(2)
            Log.d("MAIN", "$e")
        }
    }

    private fun change(){
        if (mode == 0){
            mode = 1
            callLay.isVisible = false
            cityLay.isVisible = true
            weatherLay.isVisible = true
            infoLay.isVisible = true
        }
        else{
            mode = 0
            callLay.isVisible = true
            cityLay.isVisible = false
            weatherLay.isVisible = false
            infoLay.isVisible = false
        }

    }

}