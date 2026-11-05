DROP TABLE IF EXISTS Clientes CASCADE;
DROP TABLE IF EXISTS Empleados;
DROP TABLE IF EXISTS Cuentas;
DROP TABLE IF EXISTS Cajeros;

CREATE TABLE Clientes (
                          dni varchar(9) PRIMARY KEY,
                          nombre varchar(32) NOT NULL,
                          pin varchar(4) NOT NULL,
                          rol varchar(1) DEFAULT 'C',
                          CHECK ( rol='C' )
);

INSERT INTO Clientes (dni, nombre, pin) VALUES ('12345678A', 'José Ramirez', '1234');
INSERT INTO Clientes (dni, nombre, pin) VALUES ('23456789B', 'Clara Fernández', '2345');
INSERT INTO Clientes (dni, nombre, pin) VALUES ('34567890C', 'Luís Pérez', '3456');
INSERT INTO Clientes (dni, nombre, pin) VALUES ('45678901D', 'Aitor Ferrer', '4567');
INSERT INTO Clientes (dni, nombre, pin) VALUES ('56789012E', 'Nuria Sánchez', '5678');
INSERT INTO Clientes (dni, nombre, pin) VALUES ('67890123F', 'Eva Taylor', '6789');

CREATE TABLE Empleados (
                           dni varchar(9) PRIMARY KEY,
                           nombre varchar(32) NOT NULL,
                           pin varchar(4) NOT NULL,
                           rol varchar(1) DEFAULT 'E',
                           CHECK ( rol='E' )
);

INSERT INTO Empleados (dni, nombre, pin) VALUES ('78901234G', 'Lorena Bosch', '7890');
INSERT INTO Empleados (dni, nombre, pin) VALUES ('89012345H', 'Claudia Ruiz', '8901');
INSERT INTO Empleados (dni, nombre, pin) VALUES ('90123456I', 'Pablo Contreras', '9012');
INSERT INTO Empleados (dni, nombre, pin) VALUES ('12341234J', 'Alberto Garcia', '1234');

CREATE TABLE Cuentas (
                         numero SERIAL PRIMARY KEY,
                         dni_titular varchar(9),
                         saldo integer DEFAULT 0
);

ALTER TABLE Cuentas
    ADD CONSTRAINT FK_DNI_CLIENTE FOREIGN KEY
        (dni_titular) REFERENCES Clientes(dni)
        ON DELETE CASCADE ON UPDATE CASCADE;

INSERT INTO Cuentas (dni_titular, saldo) VALUES ('12345678A', 5043);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('12345678A', 1234);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('23456789B', 32);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('34567890C', 54043);
INSERT INTO Cuentas (dni_titular) VALUES ('45678901D');
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('56789012E', 5043);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('12345678A', 1000);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('23456789B', 112000);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('67890123F', 14235);
INSERT INTO Cuentas (dni_titular, saldo) VALUES ('67890123F', 200);

CREATE TABLE Cajeros (
                         id SERIAL PRIMARY KEY,
                         direccion varchar(50) NOT NULL,
                         poblacion varchar(30) NOT NULL,
                         billetes5 integer DEFAULT 0,
                         billetes10 integer DEFAULT 0,
                         billetes20 integer DEFAULT 0,
                         billetes50 integer DEFAULT 0
);

INSERT INTO Cajeros (direccion, poblacion, billetes5, billetes10, billetes20, billetes50) VALUES ('Calle Falsa, 123', 'Barcelona', 100, 34, 50, 3);
INSERT INTO Cajeros (direccion, poblacion, billetes5, billetes10, billetes20, billetes50) VALUES ('Calle Manacor, 34', 'Palma', 50, 50, 50, 50);
INSERT INTO Cajeros (direccion, poblacion, billetes5, billetes10, billetes20, billetes50) VALUES ('Av. Larga, 543', 'Madrid', 8, 1, 4, 3);
INSERT INTO Cajeros (direccion, poblacion, billetes5, billetes10, billetes20, billetes50) VALUES ('Camí Son Rapinya, 5', 'Palma', 200, 125, 33, 64);

