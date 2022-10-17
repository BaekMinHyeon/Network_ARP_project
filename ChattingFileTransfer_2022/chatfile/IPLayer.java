import java.util.ArrayList;

public class IPLayer implements BaseLayer {
   public int nUnderLayerCount = 0;
   public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    _IP m_sHeader;
    
    private int Header_Size = 20;
    private int Max_Data = 1480;
    private int offset = 185;
    private int receivedLength = 0;
    private ArrayList<byte[]> FragmentCollect = new ArrayList();
    private byte[] SortFragment;
    
    //----------생성자-------------
    public IPLayer(String pName){
       pLayerName = pName;
       m_sHeader = new _IP();
    }
    //------------frame-------------
    private class _IP {
       byte ip_verlen; // 1byte
       byte ip_tos; // 1byte
       byte[] ip_len; // 2byte
       byte[] ip_id; // 2byte
       byte[] ip_fragoff; // 2byte
       byte ip_ttl; // 1byte
       byte ip_proto; // 1byte
       byte[] ip_cksum; // 2byte
       _IP_ADDR ip_src; // 4byte
       _IP_ADDR ip_dst; // 4byte
       // 여기까지 IP의 Header
       byte[] ip_data; // variable length data
       
       public _IP(){
          ip_verlen = 0x45;
          ip_tos = 0x00; // not use
          ip_len = new byte[2];
          ip_id = new byte[2]; // not use
          ip_fragoff = new byte[2];
          ip_ttl = 0x00; // not use
          ip_proto = 0x06;
          ip_cksum = new byte[2]; // not use
          ip_src = new _IP_ADDR();
          ip_dst = new _IP_ADDR();
          ip_data = null;
       }
    }
    
    private class _IP_ADDR {
       byte[] addr = new byte[4]; // ip_src, ip_dst
       
       public _IP_ADDR(){
             this.addr[0] = (byte) 0x00;
             this.addr[1] = (byte) 0x00;
             this.addr[2] = (byte) 0x00;
             this.addr[3] = (byte) 0x00;
       }
    }
    
    private boolean fragSend(byte[] input, int length){
       int num = length / Max_Data; // 단편화 개수
       byte[] array = new byte[Max_Data];
       int len = Max_Data;
       if(length % Max_Data != 0){ // 나머지 존재
          num++;
       }
       for(int i = 0; i < num; i++){
          if(i != num-1)
             setHeader(Max_Data, 0, 1, i * offset);
          else{
             if(length % Max_Data != 0){
                len = length % Max_Data;
                setHeader(len, 0, 0, i * offset);
                array = new byte[len];
             }
             else
                setHeader(Max_Data, 0, 0, i * offset);
          }
          System.arraycopy(input, i*Max_Data, array, 0, array.length);
          byte[] data = ObjToByte(m_sHeader, array, array.length); // header 추가
          EthernetLayer ethernetLayer = (EthernetLayer)this.GetUnderLayer(0); 
          ethernetLayer.Send(data, data.length);
       }
       return true;
    }
    
    public boolean Send(byte[] input, int length){
      /*
          * 이더넷 최대길이 1500byte, 최대 데이터 길이 = 1500 - ip헤더
       */
       
      if (length > Max_Data) { // 단편화 필요
         fragSend(input, length);
      } else { // 단편화 없음
         setHeader(length, 1, 0, 0 * offset);
         byte[] data = ObjToByte(m_sHeader, input, length); // header 추가
          EthernetLayer ethernetLayer = (EthernetLayer)this.GetUnderLayer(0); 
          ethernetLayer.Send(data, data.length);
      }
       return true;
    }
    
    public boolean arpSend(){
       ARPLayer arpLayer = (ARPLayer)this.GetUnderLayer(1);
       arpLayer.Send();
       return true;
    }
    
    private void setHeader(int length, int frag, int more, int fragoff){
       this.m_sHeader.ip_len = intToByte2(length); // tcp에서 전송되는 데이터 최대 1480이라서 2바이트 변환이면 충분
       // frag: 0(단편화 있음), 1(단편화 없음)  more: 0(마지막 데이터), 1(아직 데이터 남음)
       // ip_fragoff 16비트 중 맨 앞은 0 1비트 frag 1비트  more 13비트 fragoff
       this.m_sHeader.ip_fragoff[0] = (byte)(((frag & 0x1) << 6) | ((more & 0x1) << 5) | ((fragoff & 0x1f00) >> 8));
       this.m_sHeader.ip_fragoff[1] = (byte)(fragoff & 0xff);     
    }
    
    public boolean Receive(byte[] input){
       if(chkSrc(input) == true)
          return false;
       if(chkDst(input) == false)
          return false;
       int frag = GetFrag(input[6]);
       int more = GetMore(input[6]);
       int fragoff = GetFragoff(input[6], input[7]);
       byte[] data;
       if(frag == 1){ // 단편화 없음
          data = RemoveHeader(input, input.length);
          TCPLayer tcpLayer = (TCPLayer)this.GetUpperLayer(0);
          tcpLayer.Receive(data);
       }
       else{ // 단편화
          FragmentCollect.add(input);
          receivedLength += input.length - Header_Size;
          if(more == 0) { // // 마지막 데이터
             int last = fragoff / offset; // sequence
             SortFragment = new byte[receivedLength];
             if(sortList(last) == false)
                return false;
             
             TCPLayer tcpLayer = (TCPLayer)this.GetUpperLayer(0);
             tcpLayer.Receive(SortFragment);
             receivedLength = 0; // 초기화
          }
          
          
       }
       return true;
    }
    
    private boolean sortList(int last){
       if(FragmentCollect.size() - 1 != last)
          return false;
       for(int count = 0; count <= last; count++){
          byte[] fragdata = FragmentCollect.remove(0);
          int fragoff = GetFragoff(fragdata[6], fragdata[7]);
          int sequence = fragoff / offset;
          fragdata = RemoveHeader(fragdata, fragdata.length);
          System.arraycopy(fragdata, 0, SortFragment, sequence * Max_Data, fragdata.length);
       }
       return true;
    }
    
    private byte[] ObjToByte(_IP Header, byte[] input, int length) {//data에 헤더 붙여주기
      byte[] buf = new byte[length + Header_Size];
      buf[0] = Header.ip_verlen;
      buf[1] = Header.ip_tos;
      for(int i = 0; i < 2; i++){
         buf[2 + i] = Header.ip_len[i];
         buf[4 + i] = Header.ip_id[i];
         buf[6 + i] = Header.ip_fragoff[i];
         buf[10 + i] = Header.ip_cksum[i];
      }
      buf[8] = Header.ip_ttl;
      buf[9] = Header.ip_proto;
      for(int i = 0; i < 4; i++){
         buf[12 + i] = Header.ip_src.addr[i];
         buf[16 + i] = Header.ip_dst.addr[i];
      }
      for (int i = 0; i < length; i++)
         buf[Header_Size + i] = input[i];
      return buf;
   }
    
    private byte[] RemoveHeader(byte[] input, int length){
       byte[] buf = new byte[length - Header_Size];
       for(int dataIndex = 0; dataIndex < buf.length; dataIndex++)
          buf[dataIndex] = input[Header_Size + dataIndex];
       
       return buf;
    }
    
    private int GetFrag(byte value){
       int frag = (int)((value & 0x40) >> 6);
       return frag;
    }
    
    private int GetMore(byte value){
       int more = (int)((value & 0x20) >> 5);
       return more;
    }
    
    private int GetFragoff(byte value1, byte value2){
       int fragoff = (int)(((value1 & 0x1f) << 8) | (value2 & 0xff));
       return fragoff;
    }
    
    private byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] |= (byte) ((value & 0xff00) >> 8);
        temp[1] |= (byte) (value & 0xff);

        return temp;
    }

    private int byte2ToInt(byte value1, byte value2) {
        return (int)(((value1 & 0xff) << 8) | (value2 & 0xff));
    }
    
    public boolean chkSrc(byte[] input){ // 내가 보낸 것인지 확인
       for(int i = 0; i < 4; i++)
          if(m_sHeader.ip_src.addr[i] != input[12 + i])
             return false;
       return true;
    }
    
    public boolean chkDst(byte[] input){ // 나한테 보낸 것인지 확인
       for(int i = 0; i < 4; i++)
          if(m_sHeader.ip_src.addr[i] != input[16 + i])
             return false;
       return true;
    }
    
    public void SetIPSrcAddress(byte[] srcAddress){
       m_sHeader.ip_src.addr = srcAddress;
    }
    
    public void SetIPDstAddress(byte[] dstAddress){
       m_sHeader.ip_dst.addr = dstAddress;
    }
    
    //-------------- BaseLayer 상속-------------------
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