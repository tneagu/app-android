package org.coepi.android.cen

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import org.coepi.android.api.CENApi
import org.coepi.android.common.ApiSymptomsMapper
import org.coepi.android.common.ApiSymptomsMapperImpl
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

val apiModule = module {
    single { provideRetrofit() }
    single { provideCENApi(get()) }
    single<ApiSymptomsMapper> { ApiSymptomsMapperImpl() }
}

private fun provideRetrofit() : Retrofit {
    val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { setLevel(BODY) })
        .build()

    return Retrofit.Builder()
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://v1.api.coepi.org/")
        .client(client)
        .build()
}

private fun provideCENApi(retrofit: Retrofit): CENApi =
    retrofit.create(CENApi::class.java)
