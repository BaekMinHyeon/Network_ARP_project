
import java.io.*;
import java.util.ArrayList;

public class FileAppLayer implements BaseLayer {
    private int count = 0;
    public int nUnderLayerCount = 0;
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    private String fileName; // 파일 이름
    
    private int targetLength = 0; // 수신해야하는 파일의 총 크기
    
    private int Header_size = 8;

    private File file; // 저장할 파일
    private ArrayList<byte[]> fileByteList; // 수신한 파일 프레임(정렬 전)

    public FileAppLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        fileByteList = new ArrayList();
    }

    public class _FAPP_HEADER {
        byte[] fapp_totlen;
        byte fapp_type;
        byte[] fapp_unused;
        byte[] fapp_data;

        public _FAPP_HEADER() {
            this.fapp_totlen = new byte[4];
            this.fapp_type = 0x00;
            this.fapp_unused = new byte[3];
            this.fapp_data = null;
        }
    }

    _FAPP_HEADER m_sHeader = new _FAPP_HEADER();


    public void setFileType(int type) { // fapp_type 값을 설정
        m_sHeader.fapp_type = (byte) type;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    } // 파일이름 설정자

    // 파일 크기 설정자
    public void setFileSize(int fileSize) {
        m_sHeader.fapp_totlen[0] = (byte)(0xff&(fileSize >> 24));
        m_sHeader.fapp_totlen[1] = (byte)(0xff&(fileSize >> 16));
        m_sHeader.fapp_totlen[2] = (byte)(0xff&(fileSize >> 8));
        m_sHeader.fapp_totlen[3] = (byte)(0xff & fileSize);
    }

    public int calcFileFullLength(byte[] input) {
        int fullLength = 0;
        fullLength += (input[0] & 0xff) << 24;
        fullLength += (input[1] & 0xff) << 16;
        fullLength += (input[2] & 0xff) << 8;
        fullLength += (input[3] & 0xff);
        return fullLength;
    }


    public boolean fileInfoSend(byte[] input, int length) { // 파일 정보 송신 함수
        this.setFileType(1); // 파일 정보 송신임을 나타냄
        this.Send(input, length); // 파일 정보 송신

        return true;
    }

    public void setAndStartSendFile() {
        ChatFileDlg upperLayer = (ChatFileDlg) this.GetUpperLayer(0);
        File sendFile = upperLayer.getFile();
        int sendTotalLength; // 보내야하는 총 크기
        int sendedLength; // 현재 보낸 크기

        try (FileInputStream fileInputStream = new FileInputStream(sendFile)) {
            sendedLength = 0;
            BufferedInputStream fileReader = new BufferedInputStream(fileInputStream);
            sendTotalLength = (int)sendFile.length();
            this.setFileSize(sendTotalLength);
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setMinimum(0);
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setMaximum(sendTotalLength);
            byte[] sendData = new byte[sendTotalLength];
            // 파일 정보 송신
            this.fileInfoSend(sendFile.getName().getBytes(), sendFile.getName().getBytes().length);

            // 파일 데이터 송신
            this.setFileType(2);
            fileReader.read(sendData);
            this.Send(sendData, sendData.length);
            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendedLength += sendData.length;
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength);
            ((ChatFileDlg)this.GetUpperLayer(0)).ChattingArea.append(sendFile.getName() + "전송 완료\n");
            fileInputStream.close();
            fileReader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] RemoveCappHeader(byte[] input, int length) { // FileApp의 Header를 제거해주는 함수
        byte[] buf = new byte[length - Header_size];
        for(int dataIndex = 0; dataIndex < length - Header_size; ++dataIndex)
            buf[dataIndex] = input[Header_size + dataIndex];

        return buf;
    }
    

    public synchronized boolean Receive(byte[] input) { // 데이터를 수신 처리 함수
        byte[] data = RemoveCappHeader(input, input.length); // Header없애기
        if(checkReceiveFileInfo(input)) { // 파일의 정보를 받은 경우
            fileName = new String(data);
            fileName = fileName.trim();
            targetLength = calcFileFullLength(input); // 받아야 하는 총 크기 초기화
            file = new File("./" + fileName); //받는 경로..

            // Progressbar 초기화
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setMinimum(0);
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setMaximum(targetLength);
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setValue(0);

        } else {
        	if(targetLength != data.length){
        		((ChatFileDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
        		return false;
        	}
            fileByteList.add(data);
            try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(fileByteList.get(0));
                ((ChatFileDlg)this.GetUpperLayer(0)).ChattingArea.append(fileName + "파일 수신 및 생성 완료\n");
                fileByteList = new ArrayList();
            } catch (IOException e) {
            	((ChatFileDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
                e.printStackTrace();
            }
            ((ChatFileDlg)this.GetUpperLayer(0)).progressBar.setValue(targetLength); // Progressbar 갱신
        }
        return true;
    }

    public boolean Send(byte[] input, int length) { // 데이터 송신 함수
        byte[] bytes = this.ObjToByte(m_sHeader, input, length);
        ((EthernetLayer)this.GetUnderLayer(0)).fileSend(bytes, bytes.length);
        return true;
    }

    private byte[] ObjToByte(_FAPP_HEADER m_sHeader, byte[] input, int length) {
        byte[] buf = new byte[length + Header_size];
        buf[0] = m_sHeader.fapp_totlen[0];
        buf[1] = m_sHeader.fapp_totlen[1];
        buf[2] = m_sHeader.fapp_totlen[2];
        buf[3] = m_sHeader.fapp_totlen[3];
        buf[4] = m_sHeader.fapp_type;
        buf[5] = m_sHeader.fapp_unused[0];
        buf[6] = m_sHeader.fapp_unused[1];
        buf[7] = m_sHeader.fapp_unused[2];

        for(int dataIndex = 0; dataIndex < length; ++dataIndex)
            buf[Header_size + dataIndex] = input[dataIndex];

        return buf;
    }
    public boolean checkReceiveFileInfo(byte[] input) {
        if(input[4] == (byte)0x01)
            return true;

        return false;
    }

    @Override
    public String GetLayerName() {
        return pLayerName;
    }
    
    @Override
	public BaseLayer GetUnderLayer(int nindex) {
		if (nindex < 0 || nindex > nUnderLayerCount || nUnderLayerCount < 0)
			return null;
		return p_aUnderLayer.get(nindex);
	}
    
    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        if(nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		if (pUnderLayer == null)
			return;
		this.p_aUnderLayer.add(nUnderLayerCount++, pUnderLayer);
	}

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        if(pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }

}