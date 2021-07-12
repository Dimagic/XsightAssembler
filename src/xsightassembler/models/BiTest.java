package xsightassembler.models;


import javafx.beans.property.SimpleStringProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import xsightassembler.utils.Strings;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bi_test")
public class BiTest {

    public BiTest() {
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

    @Column(name = "lab_num", nullable = false)
    private int labNum;

    public int getLabNum() {
        return labNum;
    }

    public void setLabNum(int labNum) {
        this.labNum = labNum;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isduh_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Isduh isduh;

    public Isduh getIsduh() {
        return isduh;
    }

    public void setIsduh(Isduh isduh) {
        this.isduh = isduh;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "plug_date")
    private Date plugDate;

    public Date getPlugDate() {
        return plugDate;
    }

    public void setPlugDate(Date plugDate) {
        this.plugDate = plugDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    private Date startDate;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "unplug_date")
    private Date unplugDate;

    public Date getUnplugDate() {
        return unplugDate;
    }

    public void setUnplugDate(Date unplugDate) {
        this.unplugDate = unplugDate;
    }

    @Column(name = "status")
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Column(name = "stage")
    private int stage;

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    @Column(name = "duration")
    private int duration;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Column(name = "cooler")
    private int cooler;

    public int getCooler() {
        return cooler;
    }

    public void setCooler(int cooler) {
        this.cooler = cooler;
    }

    @Column(name = "icr")
    private int icr;

    public int getIcr() {
        return icr;
    }

    public void setIcr(int icr) {
        this.icr = icr;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
//    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BiNote> notes;

    public Set<BiNote> getNotes() {
        if (notes == null) {
            return new HashSet<>();
        }
        return notes;
    }

    public void setNotes(Set<BiNote> notes) {
        this.notes = notes;
    }

    public void addNote(BiNote note) {
        Set<BiNote> tmp = getNotes();
        tmp.add(note);
        setNotes(tmp);
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    public User getUser() {
        return user;
    }

    public String getUserLogin() {
        return user.getLogin();
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Column(name = "comment")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

//    Properties
    public SimpleStringProperty getLabNumProperty(){
        return new SimpleStringProperty(String.valueOf(getLabNum()));
    }

    public SimpleStringProperty getUnplugDateProperty() {
        if (getUnplugDate() == null) {
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(Utils.getFormattedDate(getUnplugDate()));
    }
    public SimpleStringProperty getNetNameProperty(){
//        ToDo: for test
//        return new SimpleStringProperty(getIsduh().getSn().replace("AM", "Fod") + ".xsight.com");
        return new SimpleStringProperty(getIsduh().getSn().replace("AM", "Fod"));
    }

    public SimpleStringProperty getStageProperty(){
        return new SimpleStringProperty(String.valueOf(getStage()));
    }

    public SimpleStringProperty getStatusProperty() {
        return new SimpleStringProperty(Strings.getPassFailMap().get(getStatus()));
    }

    public SimpleStringProperty getUserProperty() {
        return new SimpleStringProperty(getUser().getLogin());
    }

    public SimpleStringProperty getTypeProperty() {
        return new SimpleStringProperty(isduh.getTypeString());
    }

    public SimpleStringProperty getCommentProperty() {
        StringBuilder comments = new StringBuilder();
        if (comment != null && !getComment().trim().isEmpty()) {
            comments.append(comment + "; ");
        }
        for (BiNote note: getNotes()) {
            comments.append(note.getNote());
            comments.append("; ");
        }
        if (comments.toString().trim().isEmpty()){
            return new SimpleStringProperty("");
        }
        return new SimpleStringProperty(comments.toString());
    }
}
