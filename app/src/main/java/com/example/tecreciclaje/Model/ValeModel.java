package com.example.tecreciclaje.Model;

public class ValeModel {
    public String usuario_id;
    public String producto;
    public String estado;
    public long fecha_creacion;
    public long fecha_expiracion;
    public String imagen_url;
    public String vale_id; // Campo necesario

    public ValeModel() {
        // Requerido por Firebase
    }

    public ValeModel(String usuario_id, String producto, String estado, long fecha_creacion, long fecha_expiracion, String imagen_url, String vale_id) {
        this.usuario_id = usuario_id;
        this.producto = producto;
        this.estado = estado;
        this.fecha_creacion = fecha_creacion;
        this.fecha_expiracion = fecha_expiracion;
        this.imagen_url = imagen_url;
        this.vale_id = vale_id;
    }
}

