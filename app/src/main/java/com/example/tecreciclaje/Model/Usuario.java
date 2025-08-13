package com.example.tecreciclaje.Model;

public class Usuario {
    public String nombre;
    public String apellido;
    public String numControl;
    public String carrera;
    public String email;
    public String role;
    public String perfil;
    public String nfcUid;

    // Constructor vacío necesario para Firebase
    public Usuario() {}

    // Constructor con todos los parámetros
    public Usuario(String nombre, String apellido, String numControl, String carrera,
                   String email, String role, String perfil, String nfcUid) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.numControl = numControl;
        this.carrera = carrera;
        this.email = email;
        this.role = role;
        this.perfil = perfil;
        this.nfcUid = nfcUid;
    }

    // Getters y setters (opcional pero recomendado si los campos son privados)
}

