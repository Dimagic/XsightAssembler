package xsightassembler.models;

import javafx.beans.property.SimpleStringProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import xsightassembler.utils.IniUtils;
import xsightassembler.utils.MsgBox;
import xsightassembler.utils.Strings;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Entity
@Table(name = "isduh")
public class Isduh {

    public Isduh() {
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

    public String getFormattedDate() {
        return Utils.getFormattedDate(getDate());
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Column(name = "sn", length = 64, nullable = false, unique = true)
    private String sn;

    public String getSn() {
        return sn;
    }

    public String getIsduhSystemSn() {
        return getSn();
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    @Column(name = "comment")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "bowlModule_id", unique = true)
    private BowlModule bowlModule;

    public BowlModule getBowlModule() {
        return bowlModule;
    }

    public String getBowlModuleSn() {
        return bowlModule != null ? bowlModule.getModule() : "";
    }

    public void setBowlModule(BowlModule bowlModule) {
        this.bowlModule = bowlModule;
    }

    public void setBowlModule() {
        this.bowlModule = null;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "fanModule_id", unique = true)
    private FanModule fanModule;

    public FanModule getFanModule() {
        return fanModule;
    }

    public String getFanModuleSn() {
        return fanModule != null ? fanModule.getModule() : "";
    }

    public void setFanModule(FanModule fanModule) {
        this.fanModule = fanModule;
    }

    public void setFanModule() {
        this.fanModule = null;
    }

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "upperSensorModule_id", unique = true)
    private UpperSensorModule upperSensorModule;

    public UpperSensorModule getUpperSensorModule() {
        return upperSensorModule;
    }

    public String getUpperSensorModuleSn() {
        return upperSensorModule != null ? upperSensorModule.getModule() : "";
    }

    public void setUpperSensorModule(UpperSensorModule upperSensorModule) {
        this.upperSensorModule = upperSensorModule;
    }

    public void setUpperSensorModule() {
        this.upperSensorModule = null;
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

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<History> historySet;

    public Set<History> getHistorySet() {
        return historySet;
    }

    public void setHistorySet(Set<History> historySet) {
        this.historySet = historySet;
    }

    public void addHistory(History history) {
        Set<History> tmp = getHistorySet();
        tmp.add(history);
        setHistorySet(tmp);
    }

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<BiTest> testSet;

    public Set<BiTest> getTestSet() {
        return testSet;
    }

    public void setTestSet(Set<BiTest> testSet) {
        this.testSet = testSet;
    }

    public void addBiTest(BiTest biTest) {
        Set<BiTest> tmp = getTestSet();
        tmp.add(biTest);
        setTestSet(tmp);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id")
    private Pallet pallet;

    public Pallet getPallet() {
        return pallet;
    }

    public void setPallet(Pallet pallet) {
        this.pallet = pallet;
    }

    @Column(name = "assembly_status", columnDefinition = "int default 0")
    public int assemblyStatus;

    public int getAssemblyStatus() {
        return assemblyStatus;
    }

    public String getAssemblyStatusString() {
        return Strings.assemblyStatuses[getAssemblyStatus()];
    }

    public void setAssemblyStatus(int assemblyStatus) {
        this.assemblyStatus = assemblyStatus;
    }

    public AzimutModule getAzimutModule() {
        if (getUpperSensorModule() != null) {
            return getUpperSensorModule().getAzimutModule();
        }
        return null;
    }

    public String getAzimutModuleSn() {
        return getAzimutModule() != null ? getAzimutModule().getModule() : "";
    }

    public CameraModule getCameraModule() {
        if (getUpperSensorModule() != null) {
            return getUpperSensorModule().getCameraModule();
        }
        return null;
    }

    public String getCameraModuleSn() {
        return getCameraModule() != null ? getCameraModule().getModule() : "";
    }

    public RadarModule getRadarModule() {
        if (getUpperSensorModule() != null) {
            return getUpperSensorModule().getRadarModule();
        }
        return null;
    }

    public String getRadarModuleSn() {
        return getRadarModule() != null ? getRadarModule().getModule() : "";
    }

    public NoseModule getNoseModule() {
        if (getUpperSensorModule() != null) {
            return getUpperSensorModule().getNoseModule();
        }
        return null;
    }

    public String getNoseModuleSn() {
        return getNoseModule() != null ? getNoseModule().getModule() : "";
    }

    public String getCoolerSn() {
        if (getUpperSensorModule() != null) {
            return getUpperSensorModule().getCooler();
        }
        return "";
    }

    public String getTopSn() {
        if (getAzimutModule() != null) {
            return getAzimutModule().getTop();
        }
        return "";
    }

    public String getBoardSn() {
        if (getAzimutModule() != null) {
            return getAzimutModule().getBoard();
        }
        return "";
    }

    public String getMcuSn() {
        if (getCameraModule() != null) {
            return getCameraModule().getMcu();
        }
        return "";
    }

    public String getCameraSn() {
        if (getCameraModule() != null) {
            return getCameraModule().getCamera();
        }
        return "";
    }

    public String getHouseSn() {
        if (getCameraModule() != null) {
            return getCameraModule().getCameraHouse();
        }
        return "";
    }

    public String getComExSn() {
        if (getBowlModule() != null) {
            return getBowlModule().getComEx();
        }
        return "";
    }

    public String getCarrierSn() {
        if (getBowlModule() != null) {
            return getBowlModule().getCarrier();
        }
        return "";
    }

    public String getBreakableSn() {
        if (getBowlModule() != null) {
            return getBowlModule().getBreakable();
        }
        return "";
    }

    public String getPalletSn() {
        if (getPallet() != null) {
            return getPallet().getPalletNumber();
        }
        return "";
    }

    public List<Object> getAssemblyModules() {
        List<Object> objectList = new ArrayList<>();
        objectList.add(getFanModule());
        objectList.add(getUpperSensorModule());
        objectList.add(getNoseModule());
        objectList.add(getCameraModule());
        objectList.add(getBowlModule());
        return objectList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public SimpleStringProperty snProperty() {
        return new SimpleStringProperty(getSn());
    }

    public SimpleStringProperty fanProperty() {
        return new SimpleStringProperty(getFanModuleSn());
    }

    public SimpleStringProperty upperProperty() {
        return new SimpleStringProperty(getUpperSensorModuleSn());
    }

    public SimpleStringProperty bowlProperty() {
        return new SimpleStringProperty(getBowlModuleSn());
    }

    public SimpleStringProperty palletProperty() {
        return new SimpleStringProperty(getPalletSn());
    }

    public String getTypeString() {
        String s = getSystemType();
        if (s == null) {
            return "";
        }
        return s.trim().toUpperCase();
    }

    public String getSystemType() {
        try {
            IniUtils iniUtils = new IniUtils("strings.ini");
            HashMap<String, Pattern> pMap = iniUtils.getPatternMapByName("pManufIsduh");
            for (String key: pMap.keySet()) {
                Matcher m = pMap.get(key).matcher(getSn());
                if (m.find()) {
                    return key;
                }
            }
        } catch (IOException e) {
            MsgBox.msgException(e);
        }
        return null;
    }

    public boolean isHistoryPresent() {
        if (getHistorySet().size() > 0) {
            return true;
        }
        for (Object module: getModulesList()) {
            if (module != null) {
                try {
                    Set<History> tmp = (Set<History>) module.getClass().getMethod("getHistorySet").invoke(module);
                    if (tmp.size() > 0) {
                        return true;
                    }
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    MsgBox.msgException(e);
                }
            }
        }
        return false;
    }

    public List<Object> getModulesList() {
        List<Object> tmp = new ArrayList<>();
        tmp.add(getAzimutModule());
        tmp.add(getBowlModule());
        tmp.add(getFanModule());
        tmp.add(getUpperSensorModule());
        tmp.add(getCameraModule());
        tmp.add(getRadarModule());
        tmp.add(getNoseModule());
        return tmp;
    }

    public HashMap<String, Object> getModulesMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(getAzimutModuleSn(), getAzimutModule());
        map.put(getBowlModuleSn(), getBowlModule());
        map.put(getCameraModuleSn(), getCameraModule());
        map.put(getFanModuleSn(), getFanModule());
        map.put(getNoseModuleSn(), getNoseModule());
        map.put(getRadarModuleSn(), getRadarModule());
        map.put(getUpperSensorModuleSn(), getUpperSensorModule());
        return map;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, sn, bowlModule, fanModule, upperSensorModule);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
