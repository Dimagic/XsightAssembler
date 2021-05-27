package xsightassembler.services;

import xsightassembler.dao.CameraModuleDao;
import xsightassembler.models.CameraModule;
import xsightassembler.utils.CustomException;

import java.util.List;

public class CameraModuleService {
    private final CameraModuleDao dao = new CameraModuleDao();

    public CameraModuleService() {
    }

    public CameraModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<CameraModule> findAll() throws CustomException {
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

    public CameraModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public CameraModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }
}
