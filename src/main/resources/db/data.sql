INSERT INTO periodo_academico (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
VALUES
  (gen_random_uuid(), '2024-I',  'Primer Semestre 2024',  'CERRADO',    '2024-03-01', '2024-07-31'),
  (gen_random_uuid(), '2024-II', 'Segundo Semestre 2024', 'CERRADO',    '2024-08-01', '2024-12-20'),
  (gen_random_uuid(), '2025-I',  'Primer Semestre 2025',  'HABILITADO', '2025-03-01', '2025-07-31'),
  (gen_random_uuid(), '2025-II', 'Segundo Semestre 2025', 'HABILITADO', '2025-08-01', '2025-12-20')
ON CONFLICT (codigo) DO NOTHING;
