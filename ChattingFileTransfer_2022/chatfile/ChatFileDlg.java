import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jnetpcap.PcapIf;

public class ChatFileDlg extends JFrame implements BaseLayer {

   public int nUpperLayerCount = 0;
   public int nUnderLayerCount = 0;
   public String pLayerName = null;
   public ArrayList<BaseLayer> p_aUnderLayer = new ArrayList<BaseLayer>();
   public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
   BaseLayer UnderLayer;
   private JFileChooser jfc;
   private JButton jbt_open;
   private JButton jbt_save;
   JTextArea fileArea;
   File file;
   private static LayerManager m_LayerMgr = new LayerManager();

   private JTextField ChattingWrite;
   private JTextField ArpCacheIpWrite;
   private JTextField GratuitousArpIpWrite;
   private JTextField EntryDeviceWrite;
   private JTextField EntryIpWrite;
   private JTextField EntryEthernetWrite;
   private JTextField myEthernetWrite;
   private JTextField myIpWrite;
   private JTextField DeleteIpWrite;
   private JTextField DeleteProxyIpWrite;

   Container contentPane;

   JTextArea ChattingArea; //챗팅화면 보여주는 위치
   JTextArea srcMacAddress;
   JTextArea dstMacAddress;
   JTextArea ArpCacheTableArea;
   JTextArea ProxyArpEntryArea;
   JTextArea srcIpAddress;
   JTextArea dstIpAddress;

   JProgressBar progressBar;

   JLabel lblsrc;  // Label(이름)
   JLabel lbldst;
   JLabel lblarpcacheip;
   JLabel lblgratuitousarpip;
   JLabel lblmyethernet;
   JLabel lblmyip;
   JLabel lblsrcip;
   JLabel lbldstip;

   JButton Setting_Button; //Port번호(주소)를 입력받은 후 완료버튼설정
   JButton Chat_send_Button; //채팅화면의 채팅 입력 완료 후 data Send버튼
   JButton Cache_Table_Button;
   JButton Arp_Cache_Ip_Send_Button;
   JButton Arp_Cache_Item_Delete_Button;
   JButton Arp_Cache_All_Delete_Button;
   JButton Proxy_Arp_Add_Button;
   JButton Proxy_Arp_Delete_Button;
   JButton Gratuitous_Arp_Ip_Send_Button;
   JButton ArpDlg_Exit_Button;
   JButton ProxyArpEntry_Accept_Button;
   JButton ProxyArpEntry_Cancel_Button;
   JButton My_Address_Button;
   JButton ItemDelete_Accept_Button;
   JButton ItemDelete_Cancel_Button;
   JButton ProxyDelete_Accept_Button;
   JButton ProxyDelete_Cancel_Button;

   static JComboBox<String> NICComboBox;

   int adapterNumber = 0;

   String Text;

   boolean connection = false;
   
   private String ip;
   private String address;

   public static void main(String[] args) {
      m_LayerMgr.AddLayer(new NILayer("NI"));
      m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
      m_LayerMgr.AddLayer(new ARPLayer("ARP"));
      m_LayerMgr.AddLayer(new IPLayer("IP"));
      m_LayerMgr.AddLayer(new TCPLayer("TCP"));
      m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
      m_LayerMgr.AddLayer(new FileAppLayer("FileApp"));

      m_LayerMgr.AddLayer(new ChatFileDlg("GUI"));
      m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *ARP ( +GUI ) *IP ( -ARP *TCP ( *ChatApp ( *GUI ) *FileApp ( *GUI ) ) ) ) )");
   }

   public ChatFileDlg(String pName) {
      pLayerName = pName;

      setTitle("Chat_File_Transfer");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setBounds(250, 250, 644, 425);
      contentPane = new JPanel();
      ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
      setContentPane(contentPane);
      contentPane.setLayout(null);
      JPanel FilePanel = new JPanel();// chatting panel
      FilePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "file seding",
              TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      FilePanel.setBounds(10, 280, 360, 80);
      contentPane.add(FilePanel);
      FilePanel.setLayout(null);

      fileArea = new JTextArea("");
      fileArea.setEditable(false);
      fileArea.setBounds(10, 17, 270, 22);
      FilePanel.add(fileArea);

      jfc = new JFileChooser();
      jfc.setFileFilter(new FileNameExtensionFilter("txt", "txt"));
      jfc.setMultiSelectionEnabled(false);

      jbt_open = new JButton("열기");
      jbt_open.setEnabled(false);
      jbt_open.setBounds(290, 17, 60, 22);
      FilePanel.add(jbt_open);


      jbt_save = new JButton("전송");
      jbt_save.setEnabled(false);
      jbt_save.setBounds(290, 47, 60, 22);
      FilePanel.add(jbt_save);

      jbt_save.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            FileAppLayer fileLayer= (FileAppLayer) GetUnderLayer(1);
            fileLayer.setAndStartSendFile();
         }
      });

      jbt_open.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            if(jfc.showOpenDialog(FilePanel) == JFileChooser.APPROVE_OPTION){
               progressBar.setValue(0);
               fileArea.setText(jfc.getSelectedFile().toString());
               file = new File(jfc.getSelectedFile().toString());
               jbt_save.setEnabled(true);
            }
         }
      });

      progressBar = new JProgressBar();
      progressBar.setStringPainted(true);
      progressBar.setBounds(10, 45, 270, 25);
      FilePanel.add(progressBar);
      JPanel chattingPanel = new JPanel();// chatting panel
      chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
              TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      chattingPanel.setBounds(10, 5, 360, 276);
      contentPane.add(chattingPanel);
      chattingPanel.setLayout(null);

      JPanel chattingEditorPanel = new JPanel();// chatting write panel
      chattingEditorPanel.setBounds(10, 15, 340, 210);
      chattingPanel.add(chattingEditorPanel);
      chattingEditorPanel.setLayout(null);

      ChattingArea = new JTextArea();
      ChattingArea.setEditable(false);
      ChattingArea.setBounds(0, 0, 340, 210);
      chattingEditorPanel.add(ChattingArea);// chatting edit

      JPanel chattingInputPanel = new JPanel();// chatting write panel
      chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      chattingInputPanel.setBounds(10, 230, 250, 20);
      chattingPanel.add(chattingInputPanel);
      chattingInputPanel.setLayout(null);

      ChattingWrite = new JTextField();
      ChattingWrite.setBounds(2, 2, 250, 20);// 249
      chattingInputPanel.add(ChattingWrite);
      ChattingWrite.setColumns(10);// writing area

      JPanel settingPanel = new JPanel(); //Setting 관련 패널
      settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
              TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
      settingPanel.setBounds(380, 5, 236, 371);
      contentPane.add(settingPanel);
      settingPanel.setLayout(null);

      JPanel sourceIpAddressPanel = new JPanel();
      sourceIpAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      sourceIpAddressPanel.setBounds(10, 95, 170, 20);
      settingPanel.add(sourceIpAddressPanel);
      sourceIpAddressPanel.setLayout(null);

      lblsrcip = new JLabel("Source IP Address");
      lblsrcip.setBounds(10, 70, 170, 20); //위치 지정
      settingPanel.add(lblsrcip); //panel 추가

      srcIpAddress = new JTextArea();
      srcIpAddress.setBounds(2, 2, 170, 20);
      sourceIpAddressPanel.add(srcIpAddress);// src address

      JPanel sourceAddressPanel = new JPanel();
      sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      sourceAddressPanel.setBounds(10, 150, 170, 20);
      settingPanel.add(sourceAddressPanel);
      sourceAddressPanel.setLayout(null);

      lblsrc = new JLabel("Source Mac Address");
      lblsrc.setBounds(10, 125, 170, 20); //위치 지정
      settingPanel.add(lblsrc); //panel 추가

      srcMacAddress = new JTextArea();
      srcMacAddress.setBounds(2, 2, 170, 20);
      sourceAddressPanel.add(srcMacAddress);// src address

      JPanel destinationIpAddressPanel = new JPanel();
      destinationIpAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      destinationIpAddressPanel.setBounds(10, 207, 170, 20);
      settingPanel.add(destinationIpAddressPanel);
      destinationIpAddressPanel.setLayout(null);

      lbldstip = new JLabel("Destination IP Address");
      lbldstip.setBounds(10, 182, 190, 20);
      settingPanel.add(lbldstip);

      dstIpAddress = new JTextArea();
      dstIpAddress.setBounds(2, 2, 170, 20);
      destinationIpAddressPanel.add(dstIpAddress);// dst address

      JPanel destinationAddressPanel = new JPanel();
      destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
      destinationAddressPanel.setBounds(10, 262, 170, 20);
      settingPanel.add(destinationAddressPanel);
      destinationAddressPanel.setLayout(null);

      lbldst = new JLabel("Destination Mac Address");
      lbldst.setBounds(10, 237, 190, 20);
      settingPanel.add(lbldst);

      dstMacAddress = new JTextArea();
      dstMacAddress.setBounds(2, 2, 170, 20);
      destinationAddressPanel.add(dstMacAddress);// dst address

      JLabel NICLabel = new JLabel("NIC List");
      NICLabel.setBounds(10, 20, 170, 20);
      settingPanel.add(NICLabel);

      NICComboBox = new JComboBox();
      NICComboBox.setBounds(10, 49, 170, 20);
      settingPanel.add(NICComboBox);



      NILayer tempNiLayer = (NILayer) m_LayerMgr.GetLayer("NI"); //콤보박스 리스트에 추가하기 위한 인터페이스 객체

      for (int i = 0; i < tempNiLayer.getAdapterList().size(); i++) { //네트워크 인터페이스가 저장된 어뎁터 리스트의 사이즈만큼의 배열 생성
         //NICComboBox.addItem(((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(i).getDescription());
         PcapIf pcapIf = tempNiLayer.GetAdapterObject(i); //
         NICComboBox.addItem(pcapIf.getName()); // NIC 선택 창에 어댑터를 보여줌
      }

      NICComboBox.addActionListener(new ActionListener() { //combo박스를 눌렀을 때의 동작

         @Override
         public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            //adapterNumber = NICComboBox.getSelectedIndex();
            JComboBox jcombo = (JComboBox) e.getSource();
            adapterNumber = jcombo.getSelectedIndex();
            System.out.println("Index: " + adapterNumber);
            try {
               srcMacAddress.setText("");
               srcMacAddress.append(get_MacAddress(((NILayer) m_LayerMgr.GetLayer("NI"))
                       .GetAdapterObject(adapterNumber).getHardwareAddress()));

            } catch (IOException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
            }
         }
      });

      try {// 저절로 MAC주소 보이게하기
    	  address = get_MacAddress(((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(adapterNumber).getHardwareAddress());
          srcMacAddress.append(address);
          InetAddress local;
          local = InetAddress.getLocalHost();
          if (local == null)
        	  ip = null;
          else
        	  ip = local.getHostAddress();
          srcIpAddress.append(ip);
      } catch (IOException e1) {
         // TODO Auto-generated catch block
         e1.printStackTrace();
      }
      ;

      Setting_Button = new JButton("Setting");// setting
      Setting_Button.setBounds(60, 310, 120, 20);
      Setting_Button.addActionListener(new setAddressListener());
      settingPanel.add(Setting_Button);// setting

      Chat_send_Button = new JButton("Send");
      Chat_send_Button.setBounds(270, 230, 80, 20);
      Chat_send_Button.addActionListener(new setAddressListener());
      chattingPanel.add(Chat_send_Button);// chatting send button

      Cache_Table_Button = new JButton("Cache Table");
      Cache_Table_Button.setBounds(60, 340, 120, 20);
      Cache_Table_Button.addActionListener(new setAddressListener());
      settingPanel.add(Cache_Table_Button);

      setVisible(true);

   }

   class ArpDlg extends JFrame {
      public ArpDlg() {
         setTitle("Test ARP");
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setBounds(250, 250, 644, 465);
         contentPane = new JPanel();
         ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
         setContentPane(contentPane);
         contentPane.setLayout(null);

         JPanel myEthernetInputPanel = new JPanel();
         myEthernetInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
         myEthernetInputPanel.setBounds(140, 340, 160, 30);
         contentPane.add(myEthernetInputPanel);
         myEthernetInputPanel.setLayout(null);

         myEthernetWrite = new JTextField();
         myEthernetWrite.setBounds(2, 2, 159, 30);
         myEthernetInputPanel.add(myEthernetWrite);
         myEthernetWrite.setColumns(10);
         myEthernetWrite.setText(address);

         lblmyethernet = new JLabel("Ethernet Source");
         lblmyethernet.setBounds(20, 340, 100, 30);
         contentPane.add(lblmyethernet);

         JPanel myIpInputPanel = new JPanel();
         myIpInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
         myIpInputPanel.setBounds(140, 380, 160, 30);
         contentPane.add(myIpInputPanel);
         myIpInputPanel.setLayout(null);

         myIpWrite = new JTextField();
         myIpWrite.setBounds(2, 2, 159, 30);
         myIpInputPanel.add(myIpWrite);
         myIpWrite.setColumns(10);
         myIpWrite.setText(ip);

         lblmyip = new JLabel("IP Source");
         lblmyip.setBounds(40, 380, 100, 30);
         contentPane.add(lblmyip);

         JPanel arpCachePanel = new JPanel();
         arpCachePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder"), "ARP Cache",
                 TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
         arpCachePanel.setBounds(10, 10, 300, 320);
         contentPane.add(arpCachePanel);
         arpCachePanel.setLayout(null);

         JPanel arpCacheEditorPanel = new JPanel();
         arpCacheEditorPanel.setBounds(10, 20, 280, 210);
         arpCachePanel.add(arpCacheEditorPanel);
         arpCacheEditorPanel.setLayout(null);

         ArpCacheTableArea = new JTextArea();
         ArpCacheTableArea.setEditable(false);
         ArpCacheTableArea.setBounds(0, 0, 280, 200);
         arpCacheEditorPanel.add(ArpCacheTableArea);

         JPanel arpCacheIpInputPanel = new JPanel();
         arpCacheIpInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
         arpCacheIpInputPanel.setBounds(50, 280, 160, 30);
         arpCachePanel.add(arpCacheIpInputPanel);
         arpCacheIpInputPanel.setLayout(null);

         ArpCacheIpWrite = new JTextField();
         ArpCacheIpWrite.setBounds(2, 2, 159, 30);
         arpCacheIpInputPanel.add(ArpCacheIpWrite);
         ArpCacheIpWrite.setColumns(10);
         ArpCacheIpWrite.setText("169.254.255.131"); // I will kill you

         Arp_Cache_Item_Delete_Button = new JButton("Item Delete");
         Arp_Cache_Item_Delete_Button.setBounds(20, 230, 120, 30);
         Arp_Cache_Item_Delete_Button.addActionListener(new setAddressListener());
         arpCachePanel.add(Arp_Cache_Item_Delete_Button);

         Arp_Cache_All_Delete_Button = new JButton("All Delete");
         Arp_Cache_All_Delete_Button.setBounds(160, 230, 120, 30);
         Arp_Cache_All_Delete_Button.addActionListener(new setAddressListener());
         arpCachePanel.add(Arp_Cache_All_Delete_Button);

         Arp_Cache_Ip_Send_Button = new JButton("Send");
         Arp_Cache_Ip_Send_Button.setBounds(220, 280, 70, 30);
         Arp_Cache_Ip_Send_Button.addActionListener(new setAddressListener());
         arpCachePanel.add(Arp_Cache_Ip_Send_Button);

         lblarpcacheip = new JLabel("IP주소");
         lblarpcacheip.setBounds(10, 280, 70, 29);
         arpCachePanel.add(lblarpcacheip);

         JPanel proxyArpPanel = new JPanel();
         proxyArpPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder"), "Proxy ARP Entry",
                 TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
         proxyArpPanel.setBounds(310, 10, 300, 250);
         contentPane.add(proxyArpPanel);
         proxyArpPanel.setLayout(null);

         JPanel proxyArpEditorPanel = new JPanel();
         proxyArpEditorPanel.setBounds(10, 20, 280, 170);
         proxyArpPanel.add(proxyArpEditorPanel);
         proxyArpEditorPanel.setLayout(null);

         ProxyArpEntryArea = new JTextArea();
         ProxyArpEntryArea.setEditable(false);
         ProxyArpEntryArea.setBounds(0, 0, 280, 160);
         proxyArpEditorPanel.add(ProxyArpEntryArea);

         Proxy_Arp_Add_Button = new JButton("Add");
         Proxy_Arp_Add_Button.setBounds(20, 200, 120, 30);
         Proxy_Arp_Add_Button.addActionListener(new setAddressListener());
         proxyArpPanel.add(Proxy_Arp_Add_Button);

         Proxy_Arp_Delete_Button = new JButton("Delete");
         Proxy_Arp_Delete_Button.setBounds(160, 200, 120, 30);
         Proxy_Arp_Delete_Button.addActionListener(new setAddressListener());
         proxyArpPanel.add(Proxy_Arp_Delete_Button);

         JPanel gratuitousArpPanel = new JPanel();
         gratuitousArpPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder"), "Gratuitous ARP",
                 TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
         gratuitousArpPanel.setBounds(310, 260, 300, 70);
         contentPane.add(gratuitousArpPanel);
         gratuitousArpPanel.setLayout(null);

         JPanel gratuitousArpIpInputPanel = new JPanel();
         gratuitousArpIpInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
         gratuitousArpIpInputPanel.setBounds(50, 30, 160, 30);
         gratuitousArpPanel.add(gratuitousArpIpInputPanel);
         gratuitousArpIpInputPanel.setLayout(null);

         GratuitousArpIpWrite = new JTextField();
         GratuitousArpIpWrite.setBounds(2, 2, 159, 30);
         gratuitousArpIpInputPanel.add(GratuitousArpIpWrite);
         GratuitousArpIpWrite.setColumns(10);

         Gratuitous_Arp_Ip_Send_Button = new JButton("Send");
         Gratuitous_Arp_Ip_Send_Button.setBounds(220, 30, 70, 30);
         Gratuitous_Arp_Ip_Send_Button.addActionListener(new setAddressListener());
         gratuitousArpPanel.add(Gratuitous_Arp_Ip_Send_Button);

         lblgratuitousarpip = new JLabel("H/W");
         lblgratuitousarpip.setBounds(10, 30, 70, 29);
         gratuitousArpPanel.add(lblgratuitousarpip);

         ArpDlg_Exit_Button = new JButton("Exit");
         ArpDlg_Exit_Button.setBounds(330, 380, 100, 30);
         ArpDlg_Exit_Button.addActionListener(new setAddressListener());
         contentPane.add(ArpDlg_Exit_Button);

         My_Address_Button = new JButton("Setting");
         My_Address_Button.setBounds(330, 340, 100, 30);
         My_Address_Button.addActionListener(new setAddressListener());
         contentPane.add(My_Address_Button);

         ArpDlg_Exit_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         setVisible(true);
      }
   }

   class ProxyArpEntry extends JFrame {
      public ProxyArpEntry() {
         setTitle("Proxy ARP Entry 추가");
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setBounds(250, 250, 300, 250);
         contentPane = new JPanel();
         ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
         setContentPane(contentPane);
         contentPane.setLayout(null);

         JLabel DeviceLabel = new JLabel("Device");
         DeviceLabel.setBounds(61, 30, 50, 20);
         contentPane.add(DeviceLabel);

         JPanel EntryDeviceEditorPanel = new JPanel();
         EntryDeviceEditorPanel.setBounds(120, 30, 120, 20);
         contentPane.add(EntryDeviceEditorPanel);
         EntryDeviceEditorPanel.setLayout(null);

         EntryDeviceWrite = new JTextField();
         EntryDeviceWrite.setBounds(0, 0, 120, 20);
         EntryDeviceEditorPanel.add(EntryDeviceWrite);
         EntryDeviceWrite.setColumns(10);

         JLabel EntryIpLabel = new JLabel("IP 주소");
         EntryIpLabel.setBounds(60, 70, 50, 20);
         contentPane.add(EntryIpLabel);

         JPanel EntryIpEditorPanel = new JPanel();
         EntryIpEditorPanel.setBounds(120, 70, 120, 20);
         contentPane.add(EntryIpEditorPanel);
         EntryIpEditorPanel.setLayout(null);

         EntryIpWrite = new JTextField();
         EntryIpWrite.setBounds(0, 0, 120, 20);
         EntryIpEditorPanel.add(EntryIpWrite);
         EntryIpWrite.setColumns(10);

         JLabel EntryEthernetLabel = new JLabel("Ethernet 주소");
         EntryEthernetLabel.setBounds(23, 110, 80, 20);
         contentPane.add(EntryEthernetLabel);

         JPanel EntryEthernetEditorPanel = new JPanel();
         EntryEthernetEditorPanel.setBounds(120, 110, 120, 20);
         contentPane.add(EntryEthernetEditorPanel);
         EntryEthernetEditorPanel.setLayout(null);

         EntryEthernetWrite = new JTextField();
         EntryEthernetWrite.setBounds(0, 0, 120, 20);
         EntryEthernetEditorPanel.add(EntryEthernetWrite);
         EntryEthernetWrite.setColumns(10);

         ProxyArpEntry_Accept_Button = new JButton("OK");
         ProxyArpEntry_Accept_Button.setBounds(50, 150, 80, 30);
         ProxyArpEntry_Accept_Button.addActionListener(new setAddressListener());
         contentPane.add(ProxyArpEntry_Accept_Button);

         ProxyArpEntry_Cancel_Button = new JButton("Cancel");
         ProxyArpEntry_Cancel_Button.setBounds(160, 150, 80, 30);
         ProxyArpEntry_Cancel_Button.addActionListener(new setAddressListener());
         contentPane.add(ProxyArpEntry_Cancel_Button);

         ProxyArpEntry_Accept_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         ProxyArpEntry_Cancel_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         setVisible(true);
      }
   }

   class ItemDelete extends JFrame {
      public ItemDelete() {
         setTitle("Item Delete");
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setBounds(250, 250, 300, 250);
         contentPane = new JPanel();
         ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
         setContentPane(contentPane);
         contentPane.setLayout(null);

         JLabel DeleteIpLabel = new JLabel("IP 주소");
         DeleteIpLabel.setBounds(60, 70, 50, 20);
         contentPane.add(DeleteIpLabel);

         JPanel DeleteIpEditorPanel = new JPanel();
         DeleteIpEditorPanel.setBounds(120, 70, 120, 20);
         contentPane.add(DeleteIpEditorPanel);
         DeleteIpEditorPanel.setLayout(null);

         DeleteIpWrite = new JTextField();
         DeleteIpWrite.setBounds(0, 0, 120, 20);
         DeleteIpEditorPanel.add(DeleteIpWrite);
         DeleteIpWrite.setColumns(10);

         ItemDelete_Accept_Button = new JButton("Delete");
         ItemDelete_Accept_Button.setBounds(50, 150, 80, 30);
         ItemDelete_Accept_Button.addActionListener(new setAddressListener());
         contentPane.add(ItemDelete_Accept_Button);

         ItemDelete_Cancel_Button = new JButton("Cancel");
         ItemDelete_Cancel_Button.setBounds(160, 150, 80, 30);
         ItemDelete_Cancel_Button.addActionListener(new setAddressListener());
         contentPane.add(ItemDelete_Cancel_Button);

         ItemDelete_Accept_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         ItemDelete_Cancel_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         setVisible(true);
      }
   }

   class ProxyDelete extends JFrame {
      public ProxyDelete() {
         setTitle("Proxy Delete");
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setBounds(250, 250, 300, 250);
         contentPane = new JPanel();
         ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
         setContentPane(contentPane);
         contentPane.setLayout(null);

         JLabel DeleteProxyIpLabel = new JLabel("IP 주소");
         DeleteProxyIpLabel.setBounds(60, 70, 50, 20);
         contentPane.add(DeleteProxyIpLabel);

         JPanel DeleteProxyIpEditorPanel = new JPanel();
         DeleteProxyIpEditorPanel.setBounds(120, 70, 120, 20);
         contentPane.add(DeleteProxyIpEditorPanel);
         DeleteProxyIpEditorPanel.setLayout(null);

         DeleteProxyIpWrite = new JTextField();
         DeleteProxyIpWrite.setBounds(0, 0, 120, 20);
         DeleteProxyIpEditorPanel.add(DeleteProxyIpWrite);
         DeleteProxyIpWrite.setColumns(10);

         ProxyDelete_Accept_Button = new JButton("Delete");
         ProxyDelete_Accept_Button.setBounds(50, 150, 80, 30);
         ProxyDelete_Accept_Button.addActionListener(new setAddressListener());
         contentPane.add(ProxyDelete_Accept_Button);

         ProxyDelete_Cancel_Button = new JButton("Cancel");
         ProxyDelete_Cancel_Button.setBounds(160, 150, 80, 30);
         ProxyDelete_Cancel_Button.addActionListener(new setAddressListener());
         contentPane.add(ProxyDelete_Cancel_Button);

         ProxyDelete_Accept_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         ProxyDelete_Cancel_Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });

         setVisible(true);
      }
   }

   class setAddressListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {

         if (e.getSource() == Setting_Button) { //setting 버튼 누를 시

            if (Setting_Button.getText() == "Reset") { //reset 눌려졌을 경우,
               srcMacAddress.setText("");  //주소 공백으로 바뀜
               dstMacAddress.setText("");  //주소 공백으로 바뀜
               srcIpAddress.setText("");
               dstIpAddress.setText("");
               Setting_Button.setText("Setting"); //버튼을 누르면, setting으로 바뀜
               srcMacAddress.setEnabled(true);  //버튼을 활성화시킴
               dstMacAddress.setEnabled(true);  //버튼을 활성화시킴
               srcIpAddress.setEnabled(true);
               dstIpAddress.setEnabled(true);
               ChattingArea.setText(""); // 초기화
               fileArea.setText(""); // 초기화
               progressBar.setValue(0); // 프로그래스바 초기화
               jbt_save.setEnabled(false); // 전송 비활성화
               ((NILayer) m_LayerMgr.GetLayer("NI")).clean(); // pcap close
            }
            else { //송수신주소 설정

               byte[] byteSrcMacAddress = new byte[6];
               byte[] byteDstMacAddress = new byte[6];
               byte[] byteSrcIpAddress = new byte[6];
               byte[] byteDstIpAddress = new byte[6];

               String srcMac = srcMacAddress.getText(); //MAC 주소를 String byte로 변환
               //String dstMac = dstMacAddress.getText();
               String srcIp = srcIpAddress.getText();
               String dstIp = dstIpAddress.getText();

               String[] byte_src_mac = srcMac.split("-"); //Sting MAC 주소를"-"로 나눔
               for (int i = 0; i < 6; i++) {
                  byteSrcMacAddress[i] = (byte) Integer.parseInt(byte_src_mac[i], 16); //16비트 (2byte)
               }

//               String[] byte_dst_mac = dstMac.split("-");//Sting MAC 주소를"-"로 나눔
//               for (int i = 0; i < 6; i++) {
//                  byteDstMacAddress[i] = (byte) Integer.parseInt(byte_dst_mac[i], 16);//16비트 (2byte)
//               }

               String[] byte_src_ip = srcIp.split("\\.");
               for (int i = 0; i < 4; i++) {
                  byteSrcIpAddress[i] = (byte) Integer.parseInt(byte_src_ip[i]);
               }

               String[] byte_dst_ip = dstIp.split("\\.");
               for (int i = 0; i < 4; i++) {
                  byteDstIpAddress[i] = (byte) Integer.parseInt(byte_dst_ip[i]);
               }

               ((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SetEnetSrcAddress(byteSrcMacAddress); //이부분을 통해 선택한 주소를 프로그램 상 소스주소로 사용가능
               //((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SetEnetDstAddress(byteDstMacAddress); //이부분을 통해 선택한 주소를 프로그램 상 목적지주소로 사용가능
               ((IPLayer) m_LayerMgr.GetLayer("IP")).SetIPSrcAddress(byteSrcIpAddress);
               ((IPLayer) m_LayerMgr.GetLayer("IP")).SetIPDstAddress(byteDstIpAddress);
               ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetEnetSrcAddress(byteSrcMacAddress);
               ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetIpSrcAddress(byteSrcIpAddress);

               if ( connection == false ) {
                  ((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(adapterNumber);
                  connection = true;
               }
               
               setDstMacAddress("");
               ((ARPLayer) m_LayerMgr.GetLayer("ARP")).autoChkArp(byteDstIpAddress);
               


               jbt_open.setEnabled(true);
//               Setting_Button.setText("Reset"); //setting 버튼 누르면 리셋으로 바뀜
//               dstMacAddress.setEnabled(false);  //버튼을 비활성화시킴
//               srcMacAddress.setEnabled(false);  //버튼을 비활성화시킴
//               dstIpAddress.setEnabled(false);
//               srcIpAddress.setEnabled(false);
            }
         }

         if (e.getSource() == Chat_send_Button) { //send 버튼 누르면,
//            if (Setting_Button.getText() == "Reset") {
            String input = ChattingWrite.getText(); //채팅창에 입력된 텍스트를 저장
            ChattingArea.append("[SEND] : " + input + "\n"); //성공하면 입력값 출력
            byte[] bytes = input.getBytes(); //입력된 메시지를 바이트로 저장

            ((ChatAppLayer)m_LayerMgr.GetLayer("ChatApp")).Send(bytes, bytes.length);
            //채팅창에 입력된 메시지를 chatApplayer로 보냄
            ChattingWrite.setText("");
            //채팅 입력란 다시 비워줌
//            } else {
//               JOptionPane.showMessageDialog(null, "Address Setting Error!.");//주소설정 에러
//            }
         }

         if (e.getSource() == Cache_Table_Button) {
            new ArpDlg();
         }

         if (e.getSource() == Proxy_Arp_Add_Button) {
            new ProxyArpEntry();
         }

         if (e.getSource() == Arp_Cache_Item_Delete_Button) {
            new ItemDelete();
         }

         if (e.getSource() == Proxy_Arp_Delete_Button) {
            new ProxyDelete();
         }

         if (e.getSource() == My_Address_Button) {
            byte[] srcEthernetAddress = new byte[6];
            byte[] srcIpAddress = new byte[4];

            String srcEthernet = myEthernetWrite.getText();
            String srcIp = myIpWrite.getText();

            String[] byte_src_ethernet = srcEthernet.split("-");//Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 6; i++) {
               srcEthernetAddress[i] = (byte) Integer.parseInt(byte_src_ethernet[i], 16);//16비트 (2byte)
            }
            String[] byte_src_ip = srcIp.split("\\."); //Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 4; i++) {
               srcIpAddress[i] =  (byte) (Integer.parseInt(byte_src_ip[i])); //16비트 (2byte)
            }

            ((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SetEnetSrcAddress(srcEthernetAddress);
            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetEnetSrcAddress(srcEthernetAddress);
            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetIpSrcAddress(srcIpAddress);

            if ( connection == false ) {
               ((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(adapterNumber);
               connection = true;
            }

            myEthernetWrite.setEnabled(false);
            myIpWrite.setEnabled(false);
         }

         if (e.getSource() == Arp_Cache_Ip_Send_Button) {
            ArpCacheTableArea.append(ArpCacheIpWrite.getText() + " " + "??-??-??-??-??-??" + " " + "incomplete\n");
            byte[] dstEthernetAddress = new byte[6];
            byte[] dstIpAddress = new byte[4];

            String dstEthernet = "00-00-00-00-00-00";
            String dstIp = ArpCacheIpWrite.getText();

            String[] byte_dst_ethernet = dstEthernet.split("-");//Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 6; i++) {
               dstEthernetAddress[i] = (byte) Integer.parseInt(byte_dst_ethernet[i], 16);//16비트 (2byte)
            }

            String[] byte_dst_ip = dstIp.split("\\."); //Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 4; i++) {
               dstIpAddress[i] =  (byte) (Integer.parseInt(byte_dst_ip[i])); //16비트 (2byte)
            }

            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetEnetDstAddress(dstEthernetAddress);
            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetIpDstAddress(dstIpAddress);
            ((TCPLayer) m_LayerMgr.GetLayer("TCP")).arpSend();

            ArpCacheIpWrite.setText("");
         }

         if (e.getSource() == ProxyArpEntry_Accept_Button) {
            byte[] proxyEthernetAddress = new byte[6];
            byte[] proxyIpAddress = new byte[4];

            String proxyDevice = EntryDeviceWrite.getText();
            String proxyEthernet = EntryEthernetWrite.getText();
            String proxyIp = EntryIpWrite.getText();

            String[] byte_proxy_ethernet = proxyEthernet.split("-");//Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 6; i++) {
               proxyEthernetAddress[i] = (byte) Integer.parseInt(byte_proxy_ethernet[i], 16);//16비트 (2byte)
            }

            String[] byte_proxy_ip = proxyIp.split("\\.");
            for (int i = 0; i < 4; i++) {
               proxyIpAddress[i] = (byte) (Integer.parseInt(byte_proxy_ip[i])); //16비트 (2byte)
            }

            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).ProxyTableSet(proxyDevice, proxyEthernetAddress, proxyIpAddress);
         }

         if (e.getSource() == Gratuitous_Arp_Ip_Send_Button) {
            byte[] gratuitousEthernetAddress = new byte[6];

            String gratuitousEthernet = GratuitousArpIpWrite.getText();

            myEthernetWrite.setText(gratuitousEthernet);

            String[] byte_gratuitous_ethernet = gratuitousEthernet.split("-");
            for (int i = 0; i < 6; i++) {
               gratuitousEthernetAddress[i] = (byte) Integer.parseInt(byte_gratuitous_ethernet[i], 16);
            }

            byte[] dstIpAddress = new byte[4];
            String dstIp = ArpCacheIpWrite.getText();
            String[] byte_dst_ip = dstIp.split("\\."); //Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 4; i++) {
               dstIpAddress[i] =  (byte) (Integer.parseInt(byte_dst_ip[i])); //16비트 (2byte)
            }

            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetEnetSrcAddress(gratuitousEthernetAddress);
            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetEnetDstAddress(gratuitousEthernetAddress);
            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).SetIpDstAddress(dstIpAddress);
            ((TCPLayer) m_LayerMgr.GetLayer("TCP")).arpSend();
         }

         if (e.getSource() == ItemDelete_Accept_Button) {
            byte[] deleteIpAddress = new byte[4];

            String dstIp = DeleteIpWrite.getText();

            String[] byte_delete_ip = dstIp.split("\\."); //Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 4; i++) {
               deleteIpAddress[i] =  (byte) (Integer.parseInt(byte_delete_ip[i])); //16비트 (2byte)
            }

            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).ArpTableDelete(deleteIpAddress);
         }

         if (e.getSource() == Arp_Cache_All_Delete_Button) {
            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).ArpTableAllDelete();
         }

         if (e.getSource() == ProxyDelete_Accept_Button) {
            byte[] deleteIpAddress = new byte[4];

            String dstIp = DeleteProxyIpWrite.getText();

            String[] byte_delete_ip = dstIp.split("\\."); //Sting MAC 주소를"-"로 나눔
            for (int i = 0; i < 4; i++) {
               deleteIpAddress[i] =  (byte) (Integer.parseInt(byte_delete_ip[i])); //16비트 (2byte)
            }

            ((ARPLayer) m_LayerMgr.GetLayer("ARP")).ProxyTableDelete(deleteIpAddress);
         }
      }
   }
   public File getFile() {
      return file;
   }

   public String get_MacAddress(byte[] byte_MacAddress) { //MAC Byte주소를 String으로 변환

      String MacAddress = "";
      for (int i = 0; i < 6; i++) {
         //2자리 16진수를 대문자로, 그리고 1자리 16진수는 앞에 0을 붙임.
         MacAddress += String.format("%02X%s", byte_MacAddress[i], (i < MacAddress.length() - 1) ? "" : "");

         if (i != 5) {
            //2자리 16진수 자리 단위 뒤에 "-"붙여주기
            MacAddress += "-";
         }
      }
      System.out.println("mac_address:" + MacAddress);
      return MacAddress;
   }

   public boolean Receive(byte[] input) { //메시지 Receive
      if (input != null) {
         byte[] data = input;   //byte 단위의 input data
         Text = new String(data); //아래층에서 올라온 메시지를 String text로 변환해줌
         ChattingArea.append("[RECV] : " + Text + "\n"); //채팅창에 수신메시지를 보여줌
         return false;
      }
      return false ;
   }

   public void tablePrint(String s){
      ArpCacheTableArea.setText(s);
   }

   public void proxyTablePrint(String s) {
      ProxyArpEntryArea.setText(s);
   }

   public void setDstMacAddress(String s) {
      dstMacAddress.setText(s);
   }

   @Override
   public void SetUnderLayer(BaseLayer pUnderLayer) {
      // TODO Auto-generated method stub
      if (pUnderLayer == null)
         return;
      this.p_aUnderLayer.add(nUnderLayerCount++, pUnderLayer);
   }

   @Override
   public void SetUpperLayer(BaseLayer pUpperLayer) {
      // TODO Auto-generated method stub
      if (pUpperLayer == null)
         return;
      this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
      // nUpperLayerCount++;
   }

   @Override
   public String GetLayerName() {
      // TODO Auto-generated method stub
      return pLayerName;
   }

   public BaseLayer GetUnderLayer(int nindex) {
      // TODO Auto-generated method stub
      if (nindex < 0 || nindex > nUnderLayerCount || nUnderLayerCount < 0)
         return null;
      return p_aUnderLayer.get(nindex);
   }

   @Override
   public BaseLayer GetUpperLayer(int nindex) {
      // TODO Auto-generated method stub
      if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
         return null;
      return p_aUpperLayer.get(nindex);
   }

   @Override
   public void SetUpperUnderLayer(BaseLayer pUULayer) {
      this.SetUpperLayer(pUULayer);
      pUULayer.SetUnderLayer(this);

   }
}