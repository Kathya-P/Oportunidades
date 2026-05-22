package AsistenciaFGK.Oportunidades.models;

public class AsistenciaReporteDTO {
    private String nombreCompleto;
    private String grupo;
    private String horaEntrada;
    private String horaSalida;
    private String estado;

    public AsistenciaReporteDTO(String nombreCompleto, String grupo,
                                 String horaEntrada, String horaSalida, String estado) {
        this.nombreCompleto = nombreCompleto;
        this.grupo = grupo;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.estado = estado;
    }

    // Getters
    public String getNombreCompleto() { return nombreCompleto; }
    public String getGrupo() { return grupo; }
    public String getHoraEntrada() { return horaEntrada; }
    public String getHoraSalida() { return horaSalida; }
    public String getEstado() { return estado; }
}