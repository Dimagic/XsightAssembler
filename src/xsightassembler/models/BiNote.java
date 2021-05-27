package xsightassembler.models;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "bi_notes")
public class BiNote {
    public BiNote() {
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

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "note_date")
    private Date noteDate;

    public Date getNoteDate() {
        return noteDate;
    }

    public void setNoteDate(Date noteDate) {
        this.noteDate = noteDate;
    }

    @Column(name = "note")
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bi_test_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BiTest biTest;

    public BiTest getBiTest() {
        return biTest;
    }

    public void setBiTest(BiTest biTest) {
        this.biTest = biTest;
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

//    @Override
//    public String toString() {
//        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
//    }

//    @Override
//    public String toString() {
//        return getNote();
//    }
}
