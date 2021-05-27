package xsightassembler.models;

import javax.persistence.*;

@Entity
@Table(name = "components")
public class Component {

    public Component() {
    }

    public Component(String type, String sn) {
        this.type = type;
        this.sn = sn;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "type", length = 64, nullable = false)
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "sn", length = 64, nullable = false, unique = true)
    private String sn;

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }
}
