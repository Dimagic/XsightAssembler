package xsightassembler.services.bi;

import xsightassembler.dao.bi.BiNoteDao;
import xsightassembler.models.Isduh;
import xsightassembler.models.BiNote;
import xsightassembler.models.BiTest;
import xsightassembler.utils.CustomException;

import java.util.List;

public class BiNoteServise {

    private final BiNoteDao dao = new BiNoteDao();

    public BiNote findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<BiNote> findAll() throws CustomException {
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

    public List<BiNote> findByBiTest(BiTest biTest) throws CustomException {
        return dao.findByBiTest(biTest);
    }

    public List<BiNote> findByIsduh(Isduh isduh) throws CustomException {
        return dao.findByIsduh(isduh);
    }
}
