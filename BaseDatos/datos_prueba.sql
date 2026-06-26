INSERT INTO Usuarios (Nombres, Correo, PasswordHash, Rol) VALUES 
('Ana Torres (Host)', 'ana@uni.edu.pe', 'hash_password_123', 'Usuario'),
('Carlos Mendoza (Invitado)', 'carlos@uni.edu.pe', 'hash_password_456', 'Usuario');

-- Insertar una sala inicial creada por Ana (IdHost = 1)
INSERT INTO Salas (CodigoSala, Nombre, IdHost) VALUES 
('AULA123', 'Laboratorio de Sistemas', 1);