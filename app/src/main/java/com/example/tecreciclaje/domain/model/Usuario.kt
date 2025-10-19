package com.example.tecreciclaje.domain.model

data class Usuario(
    var usuario_id: String = "",
    var usuario_nombre: String = "",
    var usuario_apellido: String = "",
    var usuario_numControl: String = "",
    var usuario_carrera: String = "",
    var usuario_email: String = "",
    var usuario_role: String = "",
    var usuario_perfil: String = "",
    var usuario_nfcUid: String = "",
    var usuario_puntos: Int = 0,
    var usuario_tokenFCM: String = "",
    var usuario_edad: String = "",
    var usuario_provider: String = "",
    var usuario_authUid: String = ""
) {
    // Constructor vacío necesario para Firebase
    constructor() : this("", "", "", "", "", "", "", "", "", 0, "", "", "", "")
    
    // Constructor con parámetros principales
    constructor(
        id: String,
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        role: String,
        perfil: String,
        nfcUid: String
    ) : this(
        usuario_id = id,
        usuario_nombre = nombre,
        usuario_apellido = apellido,
        usuario_numControl = numControl,
        usuario_carrera = carrera,
        usuario_email = email,
        usuario_role = role,
        usuario_perfil = perfil,
        usuario_nfcUid = nfcUid,
        usuario_puntos = 0
    )

    // Propiedades computadas
    val nombreCompleto: String
        get() = "$usuario_nombre $usuario_apellido"

    val isAdmin: Boolean
        get() = usuario_role == "admin"

}
