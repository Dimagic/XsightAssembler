package xsightassembler.services.bi;

import xsightassembler.dao.bi.BiTestDao;
import xsightassembler.models.Isduh;
import xsightassembler.models.BiTest;
import xsightassembler.utils.CustomException;

import java.sql.Date;
import java.util.List;

public class BiTestService {

    private final BiTestDao dao = new BiTestDao();

    public BiTest findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<BiTest> findAll() throws CustomException {
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

    public List<BiTest> getRunningTest() throws CustomException {
        return dao.getRunningTest();
    }

    public BiTest getRunningTestByLabNum(int num) throws CustomException {
        return dao.getRunningTestByLabNum(num);
    }

    public List<BiTest> getCompleteTestBetweenDates(Date start, Date stop) throws CustomException {
        return dao.getCompleteTestBetweenDates(start, stop);
    }

    public List<BiTest> getByIsduh(Isduh isduh) throws CustomException {
        return dao.getByIsduh(isduh);
    }

    public BiTest getLastTest(Isduh isduh) throws CustomException {
        return dao.getLastTest(isduh);
    }

    public BiTest getLastRunningTest(Isduh isduh) throws CustomException {
        return dao.getLastRunningTest(isduh);
    }

}
