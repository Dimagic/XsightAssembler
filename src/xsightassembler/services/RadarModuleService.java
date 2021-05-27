package xsightassembler.services;

import xsightassembler.dao.RadarModuleDao;
import xsightassembler.models.RadarModule;
import xsightassembler.utils.CustomException;

import java.util.List;

public class RadarModuleService {

    private final RadarModuleDao dao = new RadarModuleDao();

    public RadarModuleService() {
    }

    public RadarModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<RadarModule> findAll() throws CustomException {
        return dao.findAll();
    }

    public boolean save(Object module) throws CustomException {
        return dao.save(module);
    }

    public boolean update(Object module) throws CustomException {
        return dao.update(module);
    }

    public boolean delete(Object module) throws CustomException {
        return dao.delete(module);
    }

    public boolean saveOrUpdate(Object module) throws CustomException {
        return dao.saveOrUpdate(module);
    }

    public RadarModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public RadarModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }
}
