package com.example.proyectoinnovaciong3gt2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        //Splash
        Thread.sleep(2000)
        setTheme(R.style.AppTheme)

        //para lanzar eventos personalizados
        val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message","Integración de Firebase completa")
        analytics.logEvent("InitScreen",bundle)

        //setup
        setup()


    }

    private fun setup(){
        title = "Autenticacion"

        val btnAcceder: Button = findViewById(R.id.btnRegistrar)
        val btnLogin: Button = findViewById(R.id.btnAcceder)
        val txtCorreo: EditText = findViewById(R.id.txtCorreo)
        val txtPassword: EditText = findViewById(R.id.txtPassword)
        btnAcceder.setOnClickListener{
            if(txtCorreo.text.isNotEmpty() && txtPassword.text.isNotEmpty()){
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(txtCorreo.text.toString(),
                    txtPassword.text.toString()).addOnCompleteListener{
                       if(it.isSuccessful){
                            showHome(it.result?.user?.email ?:"", ProviderType.BASIC)
                       }
                       else{
                            showAlert()
                       }
                }
            }
        }

        btnLogin.setOnClickListener{
            if(txtCorreo.text.isNotEmpty() && txtPassword.text.isNotEmpty()){
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(txtCorreo.text.toString(),
                    txtPassword.text.toString()).addOnCompleteListener{
                    if(it.isSuccessful){
                        showHome(it.result?.user?.email ?:"", ProviderType.BASIC)
                    }
                    else{
                        showAlert()
                    }
                }
            }
        }


    }

    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType){
            val homeIntent: Intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("email", email)
                putExtra("provider", provider.name)
            }
        startActivity(homeIntent)
    }

}