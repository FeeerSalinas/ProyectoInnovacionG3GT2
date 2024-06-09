package com.example.proyectoinnovaciong3gt2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class TiendaAdapter(context: Context, private val tiendas: List<Tienda>) :
    ArrayAdapter<Tienda>(context, 0, tiendas) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_tienda, parent, false)
        val tienda = tiendas[position]

        val nombreTiendaView = view.findViewById<TextView>(R.id.nombreTienda)
        val nombreDuenoView = view.findViewById<TextView>(R.id.nombreDueno)

        nombreTiendaView.text = tienda.nombreTienda
        nombreDuenoView.text = tienda.nombreDueno

        return view
    }
}