-- ============================================================
-- Sistema de Gestión de Citas Médicas - Script de base de datos (SQL Server / SSMS)
-- Ejecutar completo en SQL Server Management Studio (F5) antes de usar el sistema.
--
-- Es seguro ejecutarlo más de una vez: no borra nada y solo agrega lo que falta.
-- Deja cargado el PERSONAL PREDETERMINADO de la posta (recepcionistas, médicos y consultorios):
-- el sistema los lee al iniciar, por eso ya no se registran desde el menú.
-- Los mismos datos están en app/PersonalPredeterminado.java (respaldo si no hay conexión).
-- ============================================================
IF DB_ID('citas_medicas') IS NULL
    CREATE DATABASE citas_medicas;
GO

USE citas_medicas;
GO

-- ------------------------------------------------------------ tablas

IF OBJECT_ID('dbo.paciente', 'U') IS NULL
CREATE TABLE dbo.paciente (
    dni              CHAR(8)       NOT NULL,
    nombres          NVARCHAR(80)  NOT NULL,
    apellidos        NVARCHAR(80)  NOT NULL,
    fecha_nacimiento DATE          NOT NULL,
    telefono         VARCHAR(20)   NOT NULL DEFAULT '',
    email            VARCHAR(100)  NOT NULL DEFAULT '',
    seguro           NVARCHAR(40)  NOT NULL DEFAULT 'SIN_SEGURO',  -- nombre del enum TipoSeguro
    aportes_al_dia   BIT           NOT NULL DEFAULT 0,             -- solo importa para SIS_INDEPENDIENTE
    grupo_sanguineo  VARCHAR(5)    NOT NULL DEFAULT '',
    CONSTRAINT pk_paciente PRIMARY KEY (dni)
);
GO

IF OBJECT_ID('dbo.medico', 'U') IS NULL
CREATE TABLE dbo.medico (
    dni              CHAR(8)       NOT NULL,
    nombres          NVARCHAR(80)  NOT NULL,
    apellidos        NVARCHAR(80)  NOT NULL,
    fecha_nacimiento DATE          NOT NULL,
    telefono         VARCHAR(20)   NOT NULL DEFAULT '',
    email            VARCHAR(100)  NOT NULL DEFAULT '',
    cmp              VARCHAR(20)   NOT NULL,
    rne              VARCHAR(20)   NOT NULL DEFAULT '',            -- vacío si no es especialista
    especialidad     VARCHAR(30)   NOT NULL,                       -- nombre del enum Especialidad
    CONSTRAINT pk_medico PRIMARY KEY (dni),
    CONSTRAINT uq_medico_cmp UNIQUE (cmp)
);
GO

IF OBJECT_ID('dbo.recepcionista', 'U') IS NULL
CREATE TABLE dbo.recepcionista (
    dni              CHAR(8)       NOT NULL,
    nombres          NVARCHAR(80)  NOT NULL,
    apellidos        NVARCHAR(80)  NOT NULL,
    fecha_nacimiento DATE          NOT NULL,
    telefono         VARCHAR(20)   NOT NULL DEFAULT '',
    email            VARCHAR(100)  NOT NULL DEFAULT '',
    turno            NVARCHAR(20)  NOT NULL DEFAULT 'Mañana',
    CONSTRAINT pk_recepcionista PRIMARY KEY (dni)
);
GO

IF OBJECT_ID('dbo.consultorio', 'U') IS NULL
CREATE TABLE dbo.consultorio (
    numero INT NOT NULL,
    piso   INT NOT NULL,
    CONSTRAINT pk_consultorio PRIMARY KEY (numero)
);
GO

IF OBJECT_ID('dbo.cita', 'U') IS NULL
CREATE TABLE dbo.cita (
    id              INT            NOT NULL,   -- lo asigna la aplicación (CentroSalud)
    fecha_hora      DATETIME2(0)   NOT NULL,
    motivo          NVARCHAR(200)  NOT NULL,
    costo           DECIMAL(10,2)  NOT NULL,   -- 0.00 si el paciente está exonerado (SIS)
    estado          VARCHAR(12)    NOT NULL,   -- PROGRAMADA, CONFIRMADA, ATENDIDA, CANCELADA
    pagada          BIT            NOT NULL DEFAULT 0,
    metodo_pago     NVARCHAR(80)   NOT NULL DEFAULT '',
    dni_paciente    CHAR(8)        NOT NULL,
    dni_medico      CHAR(8)        NOT NULL,
    num_consultorio INT            NULL,
    CONSTRAINT pk_cita PRIMARY KEY (id),
    CONSTRAINT fk_cita_paciente    FOREIGN KEY (dni_paciente)    REFERENCES dbo.paciente (dni),
    CONSTRAINT fk_cita_medico      FOREIGN KEY (dni_medico)      REFERENCES dbo.medico (dni),
    CONSTRAINT fk_cita_consultorio FOREIGN KEY (num_consultorio) REFERENCES dbo.consultorio (numero)
);
GO

-- ------------------------------------------------------------ actualización de una base creada con la versión anterior

IF COL_LENGTH('dbo.medico', 'rne') IS NULL
    ALTER TABLE dbo.medico ADD rne VARCHAR(20) NOT NULL CONSTRAINT df_medico_rne DEFAULT '';
GO

IF COL_LENGTH('dbo.paciente', 'aportes_al_dia') IS NULL
    ALTER TABLE dbo.paciente ADD aportes_al_dia BIT NOT NULL CONSTRAINT df_paciente_aportes DEFAULT 0;
GO

-- ------------------------------------------------------------ personal predeterminado

INSERT INTO dbo.recepcionista (dni, nombres, apellidos, fecha_nacimiento, telefono, email, turno)
SELECT v.dni, v.nombres, v.apellidos, v.fecha_nacimiento, v.telefono, v.email, v.turno
FROM (VALUES
    ('70000001', N'Lucía', N'Ramos',  '1995-04-12', '987654321', 'lucia.ramos@postasanmartin.pe', N'Mañana'),
    ('70000002', N'Jorge', N'Vargas', '1992-08-03', '987654322', 'jorge.vargas@postasanmartin.pe', N'Tarde')
) AS v(dni, nombres, apellidos, fecha_nacimiento, telefono, email, turno)
WHERE NOT EXISTS (SELECT 1 FROM dbo.recepcionista r WHERE r.dni = v.dni);
GO

INSERT INTO dbo.medico (dni, nombres, apellidos, fecha_nacimiento, telefono, email, cmp, rne, especialidad)
SELECT v.dni, v.nombres, v.apellidos, v.fecha_nacimiento, v.telefono, v.email, v.cmp, v.rne, v.especialidad
FROM (VALUES
    ('40000001', N'Carlos', N'Mendoza', '1980-02-03', '987111001', 'carlos.mendoza@postasanmartin.pe', '45892', '',      'MEDICINA_GENERAL'),
    ('40000002', N'Marta',  N'Salazar', '1975-09-21', '987111002', 'marta.salazar@postasanmartin.pe',  '30127', '21456', 'ODONTOLOGIA'),
    ('40000003', N'Elena',  N'Paredes', '1988-06-17', '987111003', 'elena.paredes@postasanmartin.pe',  '51234', '',      'ENFERMERIA'),
    ('40000004', N'Rosa',   N'Huamán',  '1983-11-09', '987111004', 'rosa.huaman@postasanmartin.pe',    '38765', '18342', 'OBSTETRICIA'),
    ('40000005', N'Luis',   N'Quispe',  '1979-03-25', '987111005', 'luis.quispe@postasanmartin.pe',    '29418', '20977', 'EMERGENCIA')
) AS v(dni, nombres, apellidos, fecha_nacimiento, telefono, email, cmp, rne, especialidad)
WHERE NOT EXISTS (SELECT 1 FROM dbo.medico m WHERE m.dni = v.dni);
GO

INSERT INTO dbo.consultorio (numero, piso)
SELECT v.numero, v.piso
FROM (VALUES (101, 1), (102, 1), (103, 1), (201, 2), (202, 2)) AS v(numero, piso)
WHERE NOT EXISTS (SELECT 1 FROM dbo.consultorio c WHERE c.numero = v.numero);
GO
