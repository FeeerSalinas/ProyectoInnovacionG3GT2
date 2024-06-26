package com.example.proyectoinnovaciong3gt2

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class ProviderType{
    BASIC
}

class HomeActivity : AppCompatActivity(), OnMapReadyCallback{

    private val db = FirebaseFirestore.getInstance()

    private lateinit var map:GoogleMap
    private lateinit var btnCalcular:Button
    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    private var start:String = ""
    private var end:String = ""

    var poly: Polyline? = null

    companion object{
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firebase Firestore
        val db = FirebaseFirestore.getInstance()
        // Obtener referencia al fragmento del mapa y preparar el mapa

        val listView = findViewById<ListView>(R.id.listViewTiendas)
        obtenerTiendas { tiendas ->
            val adapter = TiendaAdapter(this, tiendas)
            listView.adapter = adapter
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

        //val listView = findViewById<ListView>(R.id.listViewTiendas)
        obtenerTiendas { tiendas ->
            val adapter = TiendaAdapter(this, tiendas)
            listView.adapter = adapter
        }

        Places.initialize(applicationContext,getString(R.string.google_maps_key))
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autoComplete_fragment)
                as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object :PlaceSelectionListener{
            override fun onError(p0: Status) {
                Toast.makeText(this@HomeActivity, "Ocurrió un error en la busqueda", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                //val add = place.address
                //val id = place.id
                val latLng = place.latLng
                zoomOnMap(latLng)
            }

        })

        hideAddStoreButtonForNonAdmin()
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
        enableLocation()
        loadAndAddMarkers()
    }

    //metodo que llame la interfaz
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

    //Funcion para obtener las tiendas
    fun obtenerTiendas(callback: (List<Tienda>) -> Unit) {
        db.collection("Tiendas").get()
            .addOnSuccessListener { result ->
                val tiendas = mutableListOf<Tienda>()
                for (document in result) {
                    val tienda = document.toObject(Tienda::class.java)
                    tiendas.add(tienda)
                }
                callback(tiendas)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error al obtener documentos: ", exception)
            }
    }

    //metodos para la ubicacion en tiempo real
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(this
        , Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
//metodo para la localitation
    private fun enableLocation(){
        if(!::map.isInitialized) return
        if(isLocationPermissionGranted()){
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            map.isMyLocationEnabled = true
        }
        else{
            requestLocationPermission()
        }
    }
//metodo para los permisos
    private fun requestLocationPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Se deben aceptar los permisos en ajustes", Toast.LENGTH_SHORT).show()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_CODE_LOCATION )
        }
    }
//metodo para los permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE_LOCATION -> if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                map.isMyLocationEnabled = true
            }else{
                Toast.makeText(this, "Ve a ajustes y acepta los permisos de localizacion", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }

    }

    //funcion para poder hacer zoom
    private fun zoomOnMap(latLng: LatLng){
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
        map?.animateCamera(newLatLngZoom)
    }

    // Método para cargar tiendas de Firebase y añadir marcadores
    private fun loadAndAddMarkers() {
        db.collection("Tiendas").get()
            .addOnSuccessListener { result ->
                // Limpiar el mapa de marcadores anteriores si es necesario
                map.clear()

                for (document in result) {
                    val tienda = document.toObject(Tienda::class.java)
                    val latitudStr = tienda.latitud
                    val longitudStr = tienda.longitud
                    val nombreTienda = tienda.nombreTienda

                    // Convertir a Double y verificar si es null
                    val latitud = latitudStr?.toDoubleOrNull()
                    val longitud = longitudStr?.toDoubleOrNull()

                    if (latitud != null && longitud != null) {
                        val position = LatLng(latitud, longitud)
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(nombreTienda ?: "Sin nombre")
                        )

                        // Mostrar la ventana de información del marcador
                        marker?.showInfoWindow()

                        // Programar el cierre de la ventana de información después de 2 segundos
                        val handler = Handler(Looper.getMainLooper())
                        handler.postDelayed({
                            marker?.hideInfoWindow()
                        }, 2000) // 2000 milisegundos = 2 segundos
                    } else {
                        Log.w(TAG, "Datos inválidos para la tienda: $nombreTienda")
                    }
                }

                // Configurar el InfoWindowAdapter para personalizar la ventana de información
                map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
                    override fun getInfoWindow(marker: Marker): View? {
                        // No usar una ventana personalizada predeterminada
                        return null
                    }

                    override fun getInfoContents(marker: Marker): View {
                        // Crear una vista personalizada para la ventana de información
                        val infoWindow = layoutInflater.inflate(R.layout.custom_info_window, null)
                        val title = infoWindow.findViewById<TextView>(R.id.title)
                        val snippet = infoWindow.findViewById<TextView>(R.id.snippet)

                        title.text = marker.title
                        snippet.text = marker.snippet

                        return infoWindow
                    }
                })
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error al obtener tiendas: ", exception)
            }
    }

    // Método para ocultar el botón de agregar tienda para usuarios que no sean admin
    private fun hideAddStoreButtonForNonAdmin() {
        val textViewCorreo: TextView = findViewById(R.id.textViewCorreo)
        val btnAgregarTienda: Button = findViewById(R.id.btnTiendas)

        // Obtener el correo del usuario
        val userEmail = textViewCorreo.text.toString()

        // Comparar el correo del usuario con el correo del admin
        if (userEmail != "admin@gmail.com") {
            btnAgregarTienda.visibility = View.GONE
        }
    }
}