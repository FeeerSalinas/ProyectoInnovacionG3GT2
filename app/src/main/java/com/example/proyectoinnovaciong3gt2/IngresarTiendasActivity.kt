package com.example.proyectoinnovaciong3gt2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class IngresarTiendasActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ingresar_tiendas)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnGuardar: Button = findViewById(R.id.btnAgregarTienda)
        val codigoTienda: EditText = findViewById(R.id.txtCodigoTienda)
        val nombreTienda: EditText = findViewById(R.id.txtNombreTienda)
        val nombreDueno: EditText = findViewById(R.id.txtNombreDueno)
        val telefono: EditText = findViewById(R.id.txtTelefono)
        val latitud: EditText = findViewById(R.id.txtLatitud)
        val longitud: EditText = findViewById(R.id.txtLongitud)

        btnGuardar.setOnClickListener {
            db.collection("Tiendas").document(codigoTienda.text.toString()).set(
                hashMapOf("codigoTienda" to codigoTienda.text.toString(),
                    "nombreTienda" to nombreTienda.text.toString(),
                    "nombreDueno" to nombreDueno.text.toString(),
                    "telefono" to telefono.text.toString(),
                    "latitud" to latitud.text.toString(),
                    "longitud" to longitud.text.toString())



            )

        }
    }
}