package xsightassembler.models;

import javafx.beans.property.SimpleStringProperty;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "pallets")
public class Pallet {

    public Pallet() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pallet_id")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "pallet_number", length = 32, nullable = false, unique = true)
    private String palletNumber;

    public String getPalletNumber() {
        return palletNumber;
    }

    public void setPalletNumber(String palletNumber) {
        this.palletNumber = palletNumber;
    }

    @Column(name = "comment")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "close_date")
    private Date closeDate;

    public Date getCloseDate() {
        return closeDate;
    }

    public String getFormattedDepartureDate() {
        return Utils.getFormattedDate(getCloseDate());
    }

    public void setCloseDate(Date closeDate) {
        this.closeDate = closeDate;
    }

    @OneToMany(mappedBy = "pallet")
    private List<Isduh> isduhList = new ArrayList<Isduh>();

    public List<Isduh> getIsduhList() {
        return isduhList;
    }

    public void setIsduhList(List<Isduh> isduhList) {
        this.isduhList = isduhList;
    }

    public void addIsduh(Isduh isduh) {
        List<Isduh> tmp = getIsduhList();
        tmp.add(isduh);
        isduh.setPallet(this);
        setIsduhList(tmp);
    }

    public void removeIsduh(Isduh isduh) {
        List<Isduh> tmp = getIsduhList();
        tmp.remove(isduh);
        isduh.setPallet(null);
        setIsduhList(tmp);
    }

    public SimpleStringProperty snProperty() {
        if (getPalletNumber() == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(getPalletNumber());
    }

    public SimpleStringProperty dateProperty() {
        if (getCloseDate() == null) {
            return new SimpleStringProperty("Not closed");
        }
        return new SimpleStringProperty(Utils.getFormattedDateForFolder(getCloseDate()));
    }

    public SimpleStringProperty countProperty() {
        return new SimpleStringProperty(Integer.toString(getIsduhList().size()));
    }

    public SimpleStringProperty commentProperty() {
        if (comment == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(comment);
    }

    public boolean isClosed() {
        return getCloseDate() != null;
    }

}
