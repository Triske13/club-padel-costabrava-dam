-- Borrador del esquema de PostgreSQL para Padel Costa Brava.
-- Revisado a partir del modelo ER y las reglas acordadas en B1-04.
-- Ejecutar sobre una base de datos vacia. No incluye la cuenta ADMIN inicial,
-- ya que su hash de contrasena se debe generar durante la instalacion.

CREATE TABLE socio (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(25) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    poblacion VARCHAR(100) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_socio_datos_no_vacios CHECK (
        btrim(nombre) <> '' AND btrim(apellidos) <> '' AND
        btrim(telefono) <> '' AND btrim(direccion) <> '' AND
        btrim(poblacion) <> ''
    ),
    CONSTRAINT ck_socio_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE cuenta (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    socio_id INTEGER,
    correo VARCHAR(255) NOT NULL,
    contrasena_hash TEXT,
    rol VARCHAR(5) NOT NULL,
    CONSTRAINT fk_cuenta_socio FOREIGN KEY (socio_id) REFERENCES socio(id),
    CONSTRAINT uq_cuenta_socio UNIQUE (socio_id),
    CONSTRAINT ck_cuenta_correo CHECK (correo = btrim(correo) AND correo <> ''),
    CONSTRAINT ck_cuenta_hash CHECK (
        contrasena_hash IS NULL OR btrim(contrasena_hash) <> ''
    ),
    CONSTRAINT ck_cuenta_rol_socio CHECK (
        (rol = 'USER' AND socio_id IS NOT NULL)
        OR
        (rol = 'ADMIN' AND socio_id IS NULL AND contrasena_hash IS NOT NULL)
    )
);

-- Evita correos duplicados aunque cambien mayusculas y minusculas.
CREATE UNIQUE INDEX uq_cuenta_correo_ci ON cuenta (lower(correo));

CREATE TABLE codigo_temporal (
    cuenta_id INTEGER PRIMARY KEY,
    codigo_protegido TEXT NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    caduca_en TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '15 minutes'),
    intentos SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_codigo_temporal_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta(id),
    CONSTRAINT ck_codigo_temporal_no_vacio CHECK (btrim(codigo_protegido) <> ''),
    CONSTRAINT ck_codigo_temporal_fechas CHECK (caduca_en > creado_en),
    CONSTRAINT ck_codigo_temporal_intentos CHECK (intentos BETWEEN 0 AND 3)
);

-- El servidor genera seis cifras, guarda solo su valor protegido y rechaza
-- el codigo al caducar o tras el tercer intento fallido. Un codigo nuevo
-- reemplaza la fila anterior de la misma cuenta.

CREATE TABLE pista (
    id INTEGER PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL,
    CONSTRAINT uq_pista_nombre UNIQUE (nombre),
    CONSTRAINT ck_pista_nombre_no_vacio CHECK (btrim(nombre) <> '')
);

CREATE TABLE festivo (
    fecha DATE PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL,
    CONSTRAINT ck_festivo_nombre_no_vacio CHECK (btrim(nombre) <> '')
);

CREATE TABLE reserva (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    socio_id INTEGER NOT NULL,
    pista_id INTEGER NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME(0) NOT NULL,
    duracion SMALLINT NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'CONFIRMADA',
    creada_por_cuenta_id INTEGER NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelada_en TIMESTAMPTZ,
    cancelada_por_cuenta_id INTEGER,
    motivo_cancelacion VARCHAR(200),
    CONSTRAINT fk_reserva_socio FOREIGN KEY (socio_id) REFERENCES socio(id),
    CONSTRAINT fk_reserva_pista FOREIGN KEY (pista_id) REFERENCES pista(id),
    CONSTRAINT fk_reserva_creada_por FOREIGN KEY (creada_por_cuenta_id) REFERENCES cuenta(id),
    CONSTRAINT fk_reserva_cancelada_por FOREIGN KEY (cancelada_por_cuenta_id) REFERENCES cuenta(id),
    CONSTRAINT ck_reserva_hora_entera CHECK (
        EXTRACT(MINUTE FROM hora_inicio) = 0 AND
        EXTRACT(SECOND FROM hora_inicio) = 0
    ),
    CONSTRAINT ck_reserva_duracion CHECK (duracion IN (1, 2)),
    CONSTRAINT ck_reserva_estado_cancelacion CHECK (
        (estado = 'CONFIRMADA' AND cancelada_en IS NULL AND
         cancelada_por_cuenta_id IS NULL AND motivo_cancelacion IS NULL)
        OR
        (estado = 'CANCELADA' AND cancelada_en IS NOT NULL AND
         cancelada_por_cuenta_id IS NOT NULL)
    ),
    CONSTRAINT ck_reserva_motivo_no_vacio CHECK (
        motivo_cancelacion IS NULL OR btrim(motivo_cancelacion) <> ''
    ),
    CONSTRAINT ck_reserva_orden_fechas CHECK (
        cancelada_en IS NULL OR cancelada_en >= creada_en
    )
);

CREATE TABLE bloqueo (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pista_id INTEGER NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME(0) NOT NULL,
    numero_horas SMALLINT NOT NULL,
    creada_por_cuenta_id INTEGER NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion VARCHAR(200),
    CONSTRAINT fk_bloqueo_pista FOREIGN KEY (pista_id) REFERENCES pista(id),
    CONSTRAINT fk_bloqueo_creada_por FOREIGN KEY (creada_por_cuenta_id) REFERENCES cuenta(id),
    CONSTRAINT ck_bloqueo_hora_entera CHECK (
        EXTRACT(MINUTE FROM hora_inicio) = 0 AND
        EXTRACT(SECOND FROM hora_inicio) = 0
    ),
    CONSTRAINT ck_bloqueo_numero_horas CHECK (numero_horas > 0),
    CONSTRAINT ck_bloqueo_descripcion_no_vacia CHECK (
        descripcion IS NULL OR btrim(descripcion) <> ''
    )
);

CREATE TABLE ocupacion (
    pista_id INTEGER NOT NULL,
    fecha DATE NOT NULL,
    hora TIME(0) NOT NULL,
    reserva_id INTEGER,
    bloqueo_id INTEGER,
    CONSTRAINT pk_ocupacion PRIMARY KEY (pista_id, fecha, hora),
    CONSTRAINT fk_ocupacion_pista FOREIGN KEY (pista_id) REFERENCES pista(id),
    CONSTRAINT fk_ocupacion_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id),
    CONSTRAINT fk_ocupacion_bloqueo FOREIGN KEY (bloqueo_id) REFERENCES bloqueo(id) ON DELETE CASCADE,
    CONSTRAINT ck_ocupacion_hora_entera CHECK (
        EXTRACT(MINUTE FROM hora) = 0 AND
        EXTRACT(SECOND FROM hora) = 0
    ),
    CONSTRAINT ck_ocupacion_origen CHECK (
        (reserva_id IS NOT NULL) <> (bloqueo_id IS NOT NULL)
    )
);

-- La PK de ocupacion impide duplicar pista, fecha y hora. El servidor debe
-- crear reserva/bloqueo y sus horas de ocupacion en una misma transaccion.
-- Las reglas entre tablas (cupo de un socio, horario del dia, etc.) se
-- comprueban en el servidor antes de insertar.
-- Cancelar un bloqueo antes de su inicio elimina la fila de bloqueo. La FK
-- ON DELETE CASCADE elimina sus filas de ocupacion en la misma transaccion.

-- Datos de demostracion: las cuatro pistas del proyecto.
INSERT INTO pista (id, nombre) VALUES
    (1, 'Pista 1'),
    (2, 'Pista 2'),
    (3, 'Pista 3'),
    (4, 'Pista 4');
