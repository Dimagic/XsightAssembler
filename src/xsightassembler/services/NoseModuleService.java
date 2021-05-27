package xsightassembler.services;

import xsightassembler.dao.NoseModuleDao;
import xsightassembler.models.NoseModule;
import xsightassembler.utils.CustomException;

import java.util.List;

public class NoseModuleService {

    private final NoseModuleDao dao = new NoseModuleDao();

    public NoseModule findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<NoseModule> findAll() throws CustomException {
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

    public NoseModule findBySn(String sn) throws CustomException {
        return dao.findBySn(sn);
    }

    public NoseModule findByInnerModuleSn(String sn) throws CustomException {
        return dao.findByInnerModuleSn(sn);
    }
}
