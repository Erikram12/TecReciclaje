const { onValueUpdated, onValueWritten } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");
const { onSchedule } = require("firebase-functions/v2/scheduler");

initializeApp();

/* ========== Helpers ========== */

async function getUserToken(uid) {
  // 🔥 CORREGIDO: Buscar tanto fcm_token como tokenFCM
  const snap = await getDatabase().ref(`/usuarios/${uid}`).once("value");
  const userData = snap.val();
  if (!userData) return null;
  
  // Intentar obtener el token de ambos campos posibles
  const token = userData.fcm_token || userData.tokenFCM;
  return typeof token === "string" && token.length > 0 ? token : null;
}

async function sendToToken(token, notification, data = {}) {
  if (!token) {
    console.log("No se encontró token FCM para el usuario");
    return;
  }
  
  console.log("Enviando notificación a token:", token);
  
  const message = {
    notification,
    data,
    android: { 
      priority: "high", 
      ttl: 3600 * 1000,
      notification: {
        icon: "ic_notification", // 🔥 ICONO PERSONALIZADO
        color: "#4CAF50", // Color verde para TecReciclaje
        sound: "default",
        channel_id: "TecReciclaje_Channel", // 🔥 CANAL DE NOTIFICACIONES
        priority: "high",
        default_sound: true,
        default_vibrate_timings: true,
        default_light_settings: true
      }
    },
    token,
  };
  
  try {
    await getMessaging().send(message);
    console.log("Notificación enviada exitosamente");
  } catch (error) {
    console.error("Error enviando notificación:", error);
  }
}

/* ========== 1) Notificación al ganar puntos ========== */

exports.registrarGanadosYNotificar = onValueUpdated("/usuarios/{uid}/usuario_puntos", async (event) => {
  console.log("Función registrarGanadosYNotificar ejecutada");
  
  const before = Number(event.data.before.val() || 0);
  const after  = Number(event.data.after.val()  || 0);
  const uid    = event.params.uid;

  console.log(`Puntos antes: ${before}, después: ${after}, UID: ${uid}`);

  const diff = after - before;
  if (diff <= 0) {
    console.log("No hay incremento de puntos, saliendo");
    return;
  }

  console.log(`Incremento de puntos: ${diff}`);

  // 🔥 NUEVO: Detectar si es recompensa de logro o reciclaje real
  let tipo = "ganado"; // Por defecto es reciclaje real
  let descripcion = "Reciclaje completado";
  
  // 🔥 MEJORADO: Verificar si hay un campo temporal indicando recompensa
  const userSnap = await getDatabase().ref(`/usuarios/${uid}`).once("value");
  const userData = userSnap.val();
  
  if (userData && userData.reclamando_recompensa && userData.reclamando_recompensa === diff) {
    // Es una recompensa de logro
    tipo = "recompensa";
    descripcion = `Recompensa por logro (${diff} puntos)`;
    console.log(`Detectada recompensa de logro: ${diff} puntos`);
  } else {
    // Verificar si es una recompensa de logro por valores típicos (fallback)
    const recompensasLogros = [10, 25, 50, 100, 150, 200, 30, 75];
    if (recompensasLogros.includes(diff)) {
      // 🔥 Verificar si hay un logro recién reclamado
      const logrosRef = getDatabase().ref(`/usuarios/${uid}/logros`);
      const logrosSnap = await logrosRef.once("value");
      
      if (logrosSnap.exists()) {
        let logroReclamado = null;
        logrosSnap.forEach((logroSnapshot) => {
          const logro = logroSnapshot.val();
          if (logro && logro.reclamado && logro.recompensa === diff) {
            logroReclamado = logro;
          }
        });
        
        if (logroReclamado) {
          tipo = "recompensa";
          descripcion = `Recompensa por logro: ${logroReclamado.titulo}`;
          console.log(`Detectada recompensa de logro: ${logroReclamado.titulo}`);
        }
      }
    }
  }

  // 🔥 CORREGIDO: Solo usar campos normalizados en nodo "puntos"
  await getDatabase().ref(`/usuarios/${uid}/puntos`).push({
    punto_cantidad: diff,
    punto_fecha: Date.now(),
    punto_tipo: tipo,
    punto_descripcion: descripcion,
    punto_userId: uid
  });

  // 🔥 CORREGIDO: Crear entrada separada en nodo "historial" solo si es necesario
  if (tipo === "recompensa") {
    await getDatabase().ref(`/usuarios/${uid}/historial`).push({
      historial_cantidad: diff,
      historial_fecha: Date.now(),
      historial_tipo: tipo,
      historial_descripcion: descripcion,
      historial_userId: uid
    });
  }

  // Notificación según el tipo
  let notifTitle, notifBody;
  if (tipo === "recompensa") {
    notifTitle = "🎁 ¡Recompensa de logro!";
    notifBody = `Has reclamado ${diff} puntos por tu logro. Ahora tienes ${after} puntos.`;
  } else {
    notifTitle = "♻️ ¡Puntos ganados!";
    notifBody = `Has ganado ${diff} puntos por reciclar. Ahora tienes ${after} puntos.`;
  }

  // Notificación
  const token = await getUserToken(uid);
  console.log("Token obtenido:", token ? "SÍ" : "NO");
  
  await sendToToken(
    token,
    { title: notifTitle, body: notifBody },
    { 
      tipo: tipo === "recompensa" ? "recompensa_logro" : "puntos_ganados", 
      puntos_ganados: String(diff), 
      total: String(after),
      descripcion: descripcion
    }
  );
});

/* ========== 2) Notificación a admins cuando un contenedor se llena ========== */

exports.notificarContenedorLleno = onValueUpdated("/contenedor/{tipo}/estado", async (event) => {
  const before = event.data.before.val();
  const after  = event.data.after.val();
  const tipo   = event.params.tipo;

  if (before === after) return;
  if (typeof after !== "string" || after.toLowerCase() !== "lleno") return;

  const db = getDatabase();
  const adminsSnap = await db.ref("/usuarios").orderByChild("usuario_role").equalTo("admin").once("value");
  if (!adminsSnap.exists()) return;

  const nombreAmigable =
    tipo === "contePlastico" ? "Contenedor de Plástico" :
    tipo === "conteAluminio" ? "Contenedor de Aluminio" : "Contenedor";

  const notif = { title: "Contenedor lleno", body: `${nombreAmigable} lleno. Favor de vaciarlo.` };

  const tokens = [];
  adminsSnap.forEach((u) => {
    const userData = u.val() || {};
    const tk = userData.fcm_token || userData.tokenFCM; // 🔥 CORREGIDO: Buscar ambos campos
    if (tk && typeof tk === "string" && !tokens.includes(tk)) tokens.push(tk);
  });

  await Promise.all(tokens.map((tk) =>
    sendToToken(tk, notif, { tipo: "contenedor_lleno", contenedor: String(tipo), estado: "Lleno" })
  ));
});

/* ========== 3) Escuchar cambios en /vales/{id}/estado (p.ej. Reclamado) ========== */

exports.onValeEstado = onValueUpdated("/vales/{valeId}/estado", async (event) => {
  const before = event.data.before.val();
  const after  = event.data.after.val();
  const valeId = event.params.valeId;

  if (before === after) return;
  if (typeof after !== "string") return;

  // 🔥 NUEVO: Evitar spam de notificaciones para cambios de expiración
  if ((before === "Válido" && after === "expirado") || 
      (before === "Válido" && after === "Caducado") ||
      (before === "Caducado" && after === "expirado") ||
      (before === "expirado" && after === "Caducado")) {
    console.log(`🔄 Cambio de estado de vale ${valeId}: ${before} → ${after} (no notificar)`);
    return;
  }

  // Leemos datos del vale (producto, usuario_id, etc.)
  const valeSnap = await event.data.after.ref.parent.once("value");
  if (!valeSnap.exists()) return;
  const vale = valeSnap.val() || {};
  const producto = vale.vale_producto || "Vale";
  const uid = vale.vale_usuario_id;
  const token = await getUserToken(uid);

  // Notificación por cambio de estado
  let title = "Actualización de tu vale";
  let body  = `Estado: ${after}`;
  let tipo  = "vale_estado";

  if (after === "Reclamado") {
    title = "Vale reclamado";
    body  = `Has reclamado: ${producto}`;
    tipo  = "vale_reclamado";
  } else if (after === "expirado" || after === "Caducado") {
    title = "Tu vale ha expirado";
    body  = `${producto} ya no está disponible`;
    tipo  = "vale_expirado";
  }

  await sendToToken(token, { title, body }, { tipo, valeId, estado: String(after), producto: String(producto) });
});

/* ========== 4) Monitor general de vales (caduca / 24h antes) ========== */

exports.monitorVales = onValueWritten("/vales/{valeId}", async (event) => {
  const before = event.data.before.val();
  const after  = event.data.after.val();
  const valeId = event.params.valeId;

  // Si fue borrado, nada que hacer
  if (!after) return;

  const {
    vale_usuario_id: uid,
    vale_producto: producto = "Vale",
    vale_estado: estado = "Válido",
    vale_fecha_expiracion: expMs,
    notif_24h_enviada = false,
  } = after;

  if (!uid || !expMs) return;

  const now = Date.now();
  const msEnUnDia = 24 * 60 * 60 * 1000;
  const expiraEn = Number(expMs) - now;

  // 4a) Si ya caducó y aún no marca "expirado" => marcar + notificar
  if (now >= Number(expMs) && estado !== "expirado" && estado !== "Caducado") {
    // 🔥 CORREGIDO: Solo usar campos normalizados
    await event.data.after.ref.update({ vale_estado: "expirado" });

    const token = await getUserToken(uid);
    await sendToToken(
      token,
      { title: "Tu vale ha expirado", body: `${producto} ya no está disponible.` },
      { tipo: "vale_expirado", valeId, estado: "expirado", producto: String(producto) }
    );
    return;
  }

  // 4b) Si falta < 24h, está válido y no hemos avisado aún => notificar y poner bandera
  if (expiraEn > 0 && expiraEn <= msEnUnDia && estado === "Válido" && !notif_24h_enviada) {
    const token = await getUserToken(uid);
    await sendToToken(
      token,
      { title: "⏰ Tu vale está por expirar", body: `${producto} expira en menos de 24 horas.` },
      { tipo: "vale_por_expirar", valeId, estado: String(estado), producto: String(producto) }
    );

    await event.data.after.ref.update({ notif_24h_enviada: true });
  }
});

/* ========== 5) VERIFICACIÓN AUTOMÁTICA DE EXPIRACIÓN DE VALES ========== */

// 🔥 NUEVA FUNCIÓN: Se ejecuta cada hora para verificar vales expirados
exports.verificarValesExpirados = onSchedule({
  schedule: "every 1 hours",
  region: "us-central1"
}, async (event) => {
  console.log("🕐 Ejecutando verificación automática de vales expirados...");
  
  const db = getDatabase();
  const ahora = Date.now();
  const tresDiasEnMs = 3 * 24 * 60 * 60 * 1000; // 3 días en milisegundos
  
  try {
    // 🔥 CORREGIDO: Solo usar campos normalizados
    const valesRef = db.ref('vales');
    const snapshot = await valesRef.orderByChild('vale_estado').once('value');
    
    const actualizaciones = {};
    let valesExpirados = 0;
    let valesEstandarizados = 0;
    
    snapshot.forEach((valeSnapshot) => {
      const vale = valeSnapshot.val();
      const estadoActual = vale.vale_estado;
      const fechaCreacion = vale.vale_fecha_creacion;
      const fechaExpiracion = vale.vale_fecha_expiracion;
      
      let debeExpirado = false;
      
      // Verificar por fecha de expiración específica
      if (fechaExpiracion && ahora >= Number(fechaExpiracion)) {
        debeExpirado = true;
      }
      // Verificar por fecha de creación (3 días)
      else if (fechaCreacion && (ahora - Number(fechaCreacion)) > tresDiasEnMs) {
        debeExpirado = true;
      }
      
      if (debeExpirado) {
        // 🔥 CORREGIDO: Solo usar campos normalizados
        if (estadoActual !== "expirado") {
          actualizaciones[`vales/${valeSnapshot.key}/vale_estado`] = 'expirado';
          valesExpirados++;
          
          console.log(`📅 Vale ${valeSnapshot.key} marcado como expirado`);
        }
      }
      
      // 🔥 CORREGIDO: Solo usar campos normalizados
      if (estadoActual === "Caducado") {
        actualizaciones[`vales/${valeSnapshot.key}/vale_estado`] = 'expirado';
        valesEstandarizados++;
        console.log(`🔄 Vale ${valeSnapshot.key} estandarizado de "Caducado" a "expirado"`);
      }
    });
    
    // Aplicar todas las actualizaciones de una vez
    if (Object.keys(actualizaciones).length > 0) {
      await db.ref().update(actualizaciones);
      console.log(`✅ Se actualizaron ${valesExpirados} vales como expirados y ${valesEstandarizados} estandarizados`);
    } else {
      console.log("✅ No se encontraron vales para actualizar");
    }
    
    return null;
  } catch (error) {
    console.error("❌ Error verificando vales expirados:", error);
    throw error;
  }
});

/* ========== 6) CREAR VALE CON FECHA DE EXPIRACIÓN AUTOMÁTICA ========== */

// 🔥 NUEVA FUNCIÓN: Se ejecuta cuando se crea un nuevo vale
exports.crearValeConExpiracion = onValueWritten("/vales/{valeId}", async (event) => {
  const before = event.data.before.val();
  const after = event.data.after.val();
  const valeId = event.params.valeId;

  // Solo procesar si es una creación nueva (before es null)
  if (before !== null) return;
  if (!after) return;

  console.log(`🆕 Nuevo vale creado: ${valeId}`);

  const {
    vale_usuario_id: uid,
    vale_producto: producto = "Vale",
    vale_estado: estado = "Válido",
    vale_fecha_creacion: fechaCreacion,
    vale_fecha_expiracion: fechaExpiracionExistente
  } = after;

  // Si ya tiene fecha de expiración, no hacer nada
  if (fechaExpiracionExistente) {
    console.log(`✅ Vale ${valeId} ya tiene fecha de expiración`);
    return;
  }

  try {
    // Establecer fecha de expiración automáticamente (3 días desde la creación)
    const fechaCreacionMs = fechaCreacion || Date.now();
    const fechaExpiracion = fechaCreacionMs + (3 * 24 * 60 * 60 * 1000); // 3 días
    
    // 🔥 CORREGIDO: Solo usar campos normalizados
    await event.data.after.ref.update({
      vale_fecha_expiracion: fechaExpiracion,
      vale_estado: 'Válido',
      vale_fecha_creacion: fechaCreacionMs
    });
    
    console.log(`📅 Vale ${valeId} configurado con expiración: ${new Date(fechaExpiracion).toLocaleString()}`);
    
    // 🔥 MEJORADO: Notificar solo una vez al crear el vale
    if (uid) {
      const token = await getUserToken(uid);
      await sendToToken(
        token,
        { 
          title: "🎫 ¡Nuevo vale disponible!", 
          body: `Has obtenido: ${producto}. Expira en 3 días.` 
        },
        { 
          tipo: "nuevo_vale", 
          valeId, 
          producto: String(producto),
          fecha_expiracion: String(fechaExpiracion)
        }
      );
    }
    
  } catch (error) {
    console.error(`❌ Error configurando vale ${valeId}:`, error);
  }
});

/* ========== 7) VERIFICACIÓN DIARIA DE VALES POR CADUCAR ========== */

// 🔥 NUEVA FUNCIÓN: Se ejecuta diariamente para notificar vales que caducan en 24h
exports.notificarValesPorCaducar = onSchedule({
  schedule: "every day 09:00",
  region: "us-central1"
}, async (event) => {
  console.log("🔔 Ejecutando notificación de vales por expirar...");
  
  const db = getDatabase();
  const ahora = Date.now();
  const msEnUnDia = 24 * 60 * 60 * 1000;
  
  try {
    // 🔥 CORREGIDO: Solo usar campos normalizados
    const valesRef = db.ref('vales');
    const snapshot = await valesRef.orderByChild('vale_estado').equalTo('Válido').once('value');
    
    let notificacionesEnviadas = 0;
    
    snapshot.forEach(async (valeSnapshot) => {
      const vale = valeSnapshot.val();
      const fechaExpiracion = vale.vale_fecha_expiracion;
      const notif_24h_enviada = vale.notif_24h_enviada || false;
      
      if (fechaExpiracion && !notif_24h_enviada) {
        const expiraEn = Number(fechaExpiracion) - ahora;
        
        // Si expira en menos de 24 horas
        if (expiraEn > 0 && expiraEn <= msEnUnDia) {
          const token = await getUserToken(vale.vale_usuario_id);
          
          if (token) {
            await sendToToken(
              token,
              { 
                title: "⏰ ¡Tu vale está por expirar!", 
                body: `${vale.vale_producto || 'Tu vale'} expira en menos de 24 horas. ¡Úsalo pronto!` 
              },
              { 
                tipo: "vale_por_expirar", 
                valeId: valeSnapshot.key, 
                producto: String(vale.vale_producto || 'Vale'),
                horas_restantes: String(Math.floor(expiraEn / (60 * 60 * 1000)))
              }
            );
            
            // Marcar como notificado
            await valeSnapshot.ref.update({ notif_24h_enviada: true });
            notificacionesEnviadas++;
            
            console.log(`🔔 Notificación enviada para vale ${valeSnapshot.key}`);
          }
        }
      }
    });
    
    console.log(`✅ Se enviaron ${notificacionesEnviadas} notificaciones de vales por expirar`);
    
  } catch (error) {
    console.error("❌ Error notificando vales por expirar:", error);
    throw error;
  }
});
