package app;

import modelo.CentroSalud;
import modelo.Especialidad;
import modelo.Medico;
import modelo.Recepcionista;

import java.time.LocalDate;

/**
 * Personal y consultorios de la posta, iguales a los que carga {@code sql/schema.sql}.
 * Son datos fijos: el menú no permite registrarlos. Solo se usan cuando la aplicación no puede
 * leerlos de la base de datos (sin conexión o base sin cargar), para que el sistema siga funcionando.
 */
public final class PersonalPredeterminado {

    private PersonalPredeterminado() {
    }

    public static void cargarEn(CentroSalud centro) {
        recepcionista(centro, "70000001", "Lucía", "Ramos", LocalDate.of(1995, 4, 12), "Mañana",
                "987654321", "lucia.ramos@postasanmartin.pe");
        recepcionista(centro, "70000002", "Jorge", "Vargas", LocalDate.of(1992, 8, 3), "Tarde",
                "987654322", "jorge.vargas@postasanmartin.pe");

        medico(centro, "40000001", "Carlos", "Mendoza", LocalDate.of(1980, 2, 3), "45892", "",
                Especialidad.MEDICINA_GENERAL, "987111001", "carlos.mendoza@postasanmartin.pe");
        medico(centro, "40000002", "Marta", "Salazar", LocalDate.of(1975, 9, 21), "30127", "21456",
                Especialidad.ODONTOLOGIA, "987111002", "marta.salazar@postasanmartin.pe");
        medico(centro, "40000003", "Elena", "Paredes", LocalDate.of(1988, 6, 17), "51234", "",
                Especialidad.ENFERMERIA, "987111003", "elena.paredes@postasanmartin.pe");
        medico(centro, "40000004", "Rosa", "Huamán", LocalDate.of(1983, 11, 9), "38765", "18342",
                Especialidad.OBSTETRICIA, "987111004", "rosa.huaman@postasanmartin.pe");
        medico(centro, "40000005", "Luis", "Quispe", LocalDate.of(1979, 3, 25), "29418", "20977",
                Especialidad.EMERGENCIA, "987111005", "luis.quispe@postasanmartin.pe");

        centro.agregarConsultorio(101, 1);
        centro.agregarConsultorio(102, 1);
        centro.agregarConsultorio(103, 1);
        centro.agregarConsultorio(201, 2);
        centro.agregarConsultorio(202, 2);
    }

    private static void recepcionista(CentroSalud centro, String dni, String nombres, String apellidos,
                                      LocalDate nacimiento, String turno, String telefono, String email) {
        Recepcionista r = new Recepcionista(dni, nombres, apellidos, nacimiento, turno);
        r.setTelefono(telefono);
        r.setEmail(email);
        centro.registrarRecepcionista(r);
    }

    private static void medico(CentroSalud centro, String dni, String nombres, String apellidos,
                               LocalDate nacimiento, String cmp, String rne, Especialidad especialidad,
                               String telefono, String email) {
        Medico m = new Medico(dni, nombres, apellidos, nacimiento, cmp, rne, especialidad);
        m.setTelefono(telefono);
        m.setEmail(email);
        centro.registrarMedico(m);
    }
}
