package AsistenciaFGK.Oportunidades.services;

public class RiesgoReporteDTO {
    private String nombre;
    private Long ausencias;
    private Long total;
    private String nivel;

    public RiesgoReporteDTO(String nombre, Long ausencias, Long total, String nivel) {
        this.nombre = nombre;
        this.ausencias = ausencias;
        this.total = total;
        this.nivel = nivel;
    }

    public String getNombre()    { return nombre; }
    public Long getAusencias()   { return ausencias; }
    public Long getTotal()       { return total; }
    public String getNivel()     { return nivel; }
}