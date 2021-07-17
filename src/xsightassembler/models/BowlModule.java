package xsightassembler.models;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "bowl_module")
public class BowlModule {

    public BowlModule() {
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

    @Column(name = "com_ex", length = 64, unique = true)
    private String comEx;

    public String getComEx() {
        return comEx;
    }

    public void setComEx(String comEx) {
        this.comEx = comEx;
    }

    @Column(name = "breakable", length = 64, unique = true)
    private String breakable;

    public String getBreakable() {
        return breakable;
    }

    public void setBreakable(String breakable) {
        this.breakable = breakable;
    }

    @Column(name = "carrier", length = 64, unique = true)
    private String carrier;

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    @Column(name = "flash", length = 64, unique = true)
    private String flash;

    public String getFlash() {
        return flash;
    }

    public void setFlash(String flash) {
        this.flash = flash;
    }

    @Column(name = "mac", length = 64, unique = true)
    private String mac;

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
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
    private Set<History> historySet = new HashSet<>();

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
        map.put("comEx", getComEx());
        map.put("mac", getMac());
        map.put("flash", getFlash());
        map.put("carrier", getCarrier());
        map.put("breakable", getBreakable());
        map.put("comment", getComment());
        return map;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
