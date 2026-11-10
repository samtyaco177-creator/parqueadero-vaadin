# Manual de usuario — Parqueadero Central

## Acceso
Ejecuta `mvn spring-boot:run` y abre `http://localhost:8080`.

## Panel principal
El plano muestra cada puesto. Verde significa libre y naranja significa ocupado. Selecciona un puesto para consultar su tipo y, si está ocupado, la placa, la hora de entrada y el tiempo transcurrido.

## Puestos
En el menú **Puestos** puedes crear, consultar, editar y eliminar puestos. Cada uno debe tener código único, tipo, fila y columna. El sistema no permite duplicar códigos ni posiciones, ni modificar/eliminar un puesto que esté ocupado.

## Entradas y salidas
En **Entradas y salidas**, escribe la placa, selecciona carro o moto y pulsa **Registrar entrada**. El sistema valida la placa y asigna un puesto libre del tipo correcto. Para retirar un vehículo, pulsa **Registrar salida** en su fila y confirma. Se abrirá un recibo con el tiempo y el valor calculado.

## Mensualidades
En **Mensualidades** puedes crear, editar y eliminar beneficios. Una mensualidad se considera vigente desde su fecha inicial hasta su fecha final, ambas inclusive. Si la mensualidad está vigente cuando entra el vehículo, la salida se registra sin cobro.

## Historial
En **Historial** puedes filtrar por placa, tipo y rango de fecha/hora. Deja los filtros vacíos para consultar todos los movimientos.

## Reportes y tarifas
Consulta ingresos diarios y de los últimos 30 días, y la ocupación promedio por hora de la última semana. En la misma vista puedes elegir cobro por hora (base) o por minuto (bonificación), y modificar tarifas, topes diarios y minutos gratis.

## Datos
Los registros se guardan en `data/`. No borres esos archivos mientras quieras conservar la información. Haz copias de seguridad periódicas.

## Capturas para la entrega
Después de iniciar la aplicación, toma una captura de cada una de las seis vistas y guárdalas en `docs/capturas/`. Puedes insertarlas en el README para completar el manual solicitado en el enunciado.
