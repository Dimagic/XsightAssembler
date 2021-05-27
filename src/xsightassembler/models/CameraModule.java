package xsightassembler.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "camera_module")
public class CameraModule {

    public CameraModule() {
        this.date = new Date();
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
    @Column(name = "date")
    private Date date;

    public Date getDate() {
        return date;
    }

    public String getFormattedDate(){
        return Utils.getFormattedDate(getDate());
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Column(name = "module", length = 64, nullable = false, unique = true)
    private String module;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    @Column(name = "mcu", length = 64, nullable = false, unique = true)
    private String mcu;

    public String getMcu() {
        return mcu;
    }

    public void setMcu(String mcu) {
        this.mcu = mcu;
    }

    @Column(name = "camera", length = 64, nullable = false, unique = true)
    private String camera;

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    @Column(name = "camera_house", length = 64, nullable = false, unique = true)
    private String cameraHouse;

    public String getCameraHouse() {
        return cameraHouse;
    }

    public void setCameraHouse(String cameraHouse) {
        this.cameraHouse = cameraHouse;
    }

    @Column(name = "comment")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<History> historySet;

    public Set<History> getHistorySet() {
        return historySet;
    }

    public void setHistorySet(Set<History> historySet) {
        this.historySet = historySet;
    }

    public void addHistory(History history){
        Set<History> tmp = getHistorySet();
        tmp.add(history);
        setHistorySet(tmp);
    }

    public HashMap<String, String> getValuesMap(){
        HashMap<String, String> map = new HashMap<>();
        map.put("module", getModule());
        map.put("camera", getCamera());
        map.put("house", getCameraHouse());
        map.put("mcu", getMcu());
        map.put("comment", getComment());
        return map;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
