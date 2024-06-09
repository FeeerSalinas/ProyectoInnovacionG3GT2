package com.example.proyectoinnovaciong3gt2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class ProviderType{
    BASIC
}

class HomeActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var map:GoogleMap
    private lateinit var btnCalcular:Button

    private var start:String = ""
    private var end:String = ""

    var poly: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bundle:Bundle? = intent.extras
        val email:String?= bundle?.getString("email")
        val provider:String?= bundle?.getString("provider")
        //setup()
        setup(email?:"",provider?:"")


        //para el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //instanciando boton
        btnCalcular = findViewById(R.id.btnCalcularRuta)
        btnCalcular.setOnClickListener{
            start = ""
            end = ""
            poly?.remove()
            if(poly!=null){
                poly = null
            }
            Toast.makeText(this,"Selecciona punto de origen y final", Toast.LENGTH_SHORT).show()
            if(::map.isInitialized){
                map.setOnMapClickListener {
                    if(start.isEmpty()){
                        //necesitamos las coordenadas del punti
                        start = "${it.longitude},${it.latitude}"
                    }else if(end.isEmpty()){
                        end = "${it.longitude},${it.latitude}"
                        createRoute()
                    }
                }
            }
        }

        val btnTiendas:Button = findViewById(R.id.btnTiendas)
        btnTiendas.setOnClickListener {
            val intent = Intent(this, IngresarTiendasActivity::class.java)
            startActivity(intent)
        }
    }

    //para el login
    private fun setup(email: String, provider: String){
        title = "Inicio"
        val textViewEmail:TextView = findViewById(R.id.textViewCorreo)
        textViewEmail.text = email
        val textViewProvider:TextView = findViewById(R.id.textViewProveedor)
        textViewProvider.text = provider

        val btnCerrarSesion:Button = findViewById(R.id.btnCerrarSesion)
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

    }

    //para que funcione el mapa
    override fun onMapReady(map: GoogleMap) {
        this.map = map
    }

    //metodoq que llame la interfaz
    fun createRoute(){
        Log.i("aris",start)
        Log.i("aris",end)
        
        CoroutineScope(Dispatchers.IO).launch{
            val call = getRetrofit().create(ApiService::class.java).getRout("5b3ce3597851110001cf6248d35e3138325041e498f6e8506aa75c55",start,end)
            if(call.isSuccessful){
                drawRoute(call.body())
                
            } else{
                Log.i("aris","KO")
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
             poly = map.addPolyline(polyLineOptions)
        }

    }


    //metodo que devuelva retrofit
    fun getRetrofit():Retrofit{
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}