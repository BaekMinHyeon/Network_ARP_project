import java.util.ArrayList;

//port를 확인해서 chatapp인지,fileapp인지 구분하여 전달
public class TCPLayer implements BaseLayer {

	int nUnderLayerCount = 0;
	int nUpperLayerCount = 0;
	String pLayerName = null;
	ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
	ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	_TCP_Frame m_sHeader;
	int Header_size = 24;
	
	static int CHAT_PORT = 0;
	static int FILE_PORT = 1;

	private class _TCP_Frame {
		byte[] tcp_sport;
		byte[] tcp_dport;
		byte[] tcp_seq;
		byte[] tcp_ack;
		byte tcp_offset;
		byte tcp_flag;
		byte[] tcp_window;
		byte[] tcp_cksum;
		byte[] tcp_urgptr;
		byte[] padding;
		byte[] tcp_data;

		public _TCP_Frame() {
			tcp_sport = new byte[2];
			tcp_dport = new byte[2];
			tcp_seq = new byte[4];
			tcp_ack = new byte[4];
			tcp_offset = 0x00;
			tcp_flag = 0x00;
			tcp_window = new byte[2];
			tcp_cksum = new byte[2];
			tcp_urgptr = new byte[2];
			padding = new byte[4];
			tcp_data = null;
		}
	}

	public TCPLayer(String pName) {
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		m_sHeader = new _TCP_Frame();
	}

	public boolean fileSend(byte[] input, int length) {
		m_sHeader.tcp_sport = intToByte2(FILE_PORT);
		m_sHeader.tcp_dport = intToByte2(FILE_PORT);
		return Send(input, length, 8);
	}

	public boolean chatSend(byte[] input, int length) {
		m_sHeader.tcp_sport = intToByte2(CHAT_PORT);
		m_sHeader.tcp_dport = intToByte2(CHAT_PORT);
		return Send(input, length, 4);
	}

	public boolean Send(byte[] input, int length, int len) {
		/* 이더넷 최대길이 1500byte
		 * 최대 데이터 길이  = 1500 -((파일or챗 헤더)+tcp헤더+ip헤더)*/
		int maxDataLen = 1500-(len+24+20);
		byte[] bytes;
		if (length > maxDataLen) {
			fragSend(input, length, maxDataLen);
		} else {
			m_sHeader.tcp_flag = 0x00;// 단편화 안 된 경우
			bytes = ObjToByte(m_sHeader, input, length);
			this.GetUnderLayer(0).Send(bytes, bytes.length);
		}
		return true;
	}

	private void fragSend(byte[] input, int length, int maxDataLen) {

		byte[] bytes = new byte[maxDataLen];
		int i = 0;

		// 첫번째 전송
		m_sHeader.tcp_seq = intToByte2(i);
		m_sHeader.tcp_flag = 0x01;
		System.arraycopy(input, 0, bytes, 0, maxDataLen);
		bytes = ObjToByte(m_sHeader, bytes, maxDataLen);
		this.GetUnderLayer(0).Send(bytes, bytes.length);

		int maxLen = length / maxDataLen;

		int len = maxDataLen;
		if (length % maxDataLen != 0) {
			len = length % maxDataLen;
			maxLen++;
		}
		for (i = 1; i < maxLen; i++) {
			m_sHeader.tcp_seq = intToByte2(i);
			if (i == maxLen - 1) {
				m_sHeader.tcp_flag = (byte) (0x02);
				if (len != maxDataLen) {
					bytes = new byte[len];
				}
				System.arraycopy(input, maxDataLen * i, bytes, 0, len);
				bytes = ObjToByte(m_sHeader, bytes, len);
				this.GetUnderLayer(0).Send(bytes, bytes.length);
			} else {
				System.arraycopy(input, maxDataLen * i, bytes, 0, maxDataLen);
				bytes = ObjToByte(m_sHeader, bytes, maxDataLen);
				this.GetUnderLayer(0).Send(bytes, bytes.length);
			}
		}
	}

	public boolean arpSend(byte[] dstAddr) {
		this.GetUnderLayer(0).arpSend(dstAddr);
		return true;
	}

	public synchronized boolean Receive(byte[] input) {
		byte[] data;
		int sport = byte2ToInt(input[0], input[1]);
		int dport = byte2ToInt(input[2], input[3]);
		ChatAppLayer chatAppLayer = (ChatAppLayer) this.GetUpperLayer(0);
		FileAppLayer fileAppLayer = (FileAppLayer) this.GetUpperLayer(1);
		if (sport == dport) {
			data = RemoveTCPHeader(input, input.length);
			if (dport == CHAT_PORT) {
				chatAppLayer.Receive(data);
			} else if (dport == FILE_PORT) {
				fileAppLayer.Receive(data);
			}
		}
		return false;
	}

	public byte[] ObjToByte(_TCP_Frame Header, byte[] input, int length) {// data에
																			// 헤더
																			// 붙여주기
		byte[] buf = new byte[length + Header_size];
		for (int i = 0; i < 2; i++) {
			buf[i] = Header.tcp_sport[i];
			buf[i + 2] = Header.tcp_dport[i];
		}
		for (int i = 0; i < 4; i++) {
			buf[i + 4] = Header.tcp_seq[i];
			buf[i + 8] = Header.tcp_ack[i];
		}
		buf[12] = Header.tcp_offset;
		buf[13] = Header.tcp_flag;
		for (int i = 0; i < 2; i++) {
			buf[i + 14] = Header.tcp_window[i];
			buf[i + 16] = Header.tcp_cksum[i];
			buf[i + 18] = Header.tcp_urgptr[i];
		}
		for (int i = 0; i < 4; i++)
			buf[i + 20] = Header.padding[i];

		for (int i = 0; i < length; i++)
			buf[24 + i] = input[i];

		return buf;
	}

	public byte[] RemoveTCPHeader(byte[] input, int length) {
		byte[] cpyInput = new byte[length - Header_size];
		System.arraycopy(input, Header_size, cpyInput, 0, length - Header_size);
		input = cpyInput;
		return input;
	}

	private byte[] intToByte2(int value) {
		byte[] temp = new byte[2];
		temp[0] |= (byte) ((value & 0xFF00) >> 8);
		temp[1] |= (byte) (value & 0xFF);

		return temp;
	}

	private int byte2ToInt(byte value1, byte value2) {
		return (int) (((value1 & 0xff) << 8) | (value2 & 0xff));
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
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
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
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

}
