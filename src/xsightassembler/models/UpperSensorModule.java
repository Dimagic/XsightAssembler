package xsightassembler.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "upper_sensor_module")
public class UpperSensorModule {

    public UpperSensorModule() {
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

    @Column(name = "cooler", length = 64, unique = true)
    private String cooler;

    public String getCooler() {
        return cooler;
    }

    public void setCooler(String cooler) {
        this.cooler = cooler;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name="cameraModule_id", unique = true)
    private CameraModule cameraModule;

    public CameraModule getCameraModule() {
        return cameraModule;
    }

    public String getCameraModuleSn() {
        return cameraModule != null ? cameraModule.getModule(): "";
    }

    public void setCameraModule(CameraModule cameraModule) {
        this.cameraModule = cameraModule;
    }

    public void setCameraModule() {
        this.cameraModule = null;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name="azimutModule_id", unique = true)
    private AzimutModule azimutModule;

    public AzimutModule getAzimutModule() {
        return azimutModule;
    }

    public String getAzimutModuleSn() {
        return azimutModule != null ? azimutModule.getModule(): "";
    }

    public void setAzimutModule(AzimutModule azimutModule) {
        this.azimutModule = azimutModule;
    }

    public void setAzimutModule() {
        this.azimutModule = null;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name="radarModule_id", unique = true)
    private RadarModule radarModule;

    public RadarModule getRadarModule() {
        return radarModule;
    }

    public String getRadarModuleSn() {
        return radarModule != null ? radarModule.getModule(): "";
    }

    public void setRadarModule(RadarModule radarModule) {
        this.radarModule = radarModule;
    }

    public void setRadarModule() {
        this.radarModule = null;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name="noseModule_id", unique = true)
    private NoseModule noseModule;

    public NoseModule getNoseModule() {
        return noseModule;
    }

    public String getNoseModuleSn() {
        return noseModule != null ? noseModule.getModule(): "";
    }

    public void setNoseModule(NoseModule noseModule) {
        this.noseModule = noseModule;
    }

    public void setNoseModule() {
        this.noseModule = null;
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
        map.put("cooler", getCooler());
        map.put(getRadarModuleSn(), getRadarModuleSn());
        map.put("comment", getComment());
        return map;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
