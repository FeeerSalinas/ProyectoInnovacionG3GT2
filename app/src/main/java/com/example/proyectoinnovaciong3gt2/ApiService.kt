package com.example.proyectoinnovaciong3gt2


import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/v2/directions/driving-car")
    suspend fun getRout(
                @Query("api_key") key:String,
                @Query("start", encoded = true) start:String,
                @Query("end", encoded = true) end:String
    ):Response<RouteResponse>
}