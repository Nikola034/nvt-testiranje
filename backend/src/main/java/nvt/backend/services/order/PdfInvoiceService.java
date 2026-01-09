package nvt.backend.services.order;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.model.order.Order;
import nvt.backend.model.order.OrderItem;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfInvoiceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateInvoice(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Header
            addHeader(document, order);
            
            // Company info (seller and buyer)
            addCompanyInfo(document, order);
            
            // Items table
            addItemsTable(document, order);
            
            // Totals
            addTotals(document, order);
            
            // Footer
            addFooter(document, order);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF invoice for order {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
    }

    private void addHeader(Document document, Order order) throws DocumentException {
        // Title
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(33, 37, 41));
        Paragraph title = new Paragraph("FAKTURA", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Invoice number and date
        Font infoFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
        Paragraph invoiceInfo = new Paragraph();
        invoiceInfo.setAlignment(Element.ALIGN_CENTER);
        invoiceInfo.add(new Chunk("Broj fakture: " + order.getInvoiceNumber(), infoFont));
        invoiceInfo.add(Chunk.NEWLINE);
        invoiceInfo.add(new Chunk("Datum: " + order.getCreatedAt().format(DATE_FORMATTER), infoFont));
        invoiceInfo.add(Chunk.NEWLINE);
        invoiceInfo.add(new Chunk("Broj narudžbine: " + order.getOrderNumber(), infoFont));
        document.add(invoiceInfo);
        
        document.add(new Paragraph("\n"));
    }

    private void addCompanyInfo(Document document, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        
        // Seller info
        PdfPCell sellerCell = new PdfPCell();
        sellerCell.setBorder(Rectangle.BOX);
        sellerCell.setPadding(10f);
        sellerCell.setBackgroundColor(new Color(248, 249, 250));
        
        Paragraph seller = new Paragraph();
        seller.add(new Chunk("PRODAVAC\n", headerFont));
        seller.add(new Chunk("Smartly d.o.o.\n", normalFont));
        seller.add(new Chunk("PIB: 123456789\n", normalFont));
        seller.add(new Chunk("Matični broj: 12345678\n", normalFont));
        seller.add(new Chunk("Adresa: Bulevar oslobođenja 1\n", normalFont));
        seller.add(new Chunk("21000 Novi Sad, Srbija\n", normalFont));
        seller.add(new Chunk("Email: prodaja@smartly.com\n", normalFont));
        sellerCell.addElement(seller);
        table.addCell(sellerCell);
        
        // Buyer info
        PdfPCell buyerCell = new PdfPCell();
        buyerCell.setBorder(Rectangle.BOX);
        buyerCell.setPadding(10f);
        buyerCell.setBackgroundColor(new Color(248, 249, 250));
        
        var company = order.getDeliveryCompany();
        var customer = order.getCustomer();
        
        Paragraph buyer = new Paragraph();
        buyer.add(new Chunk("KUPAC\n", headerFont));
        buyer.add(new Chunk(company.getName() + "\n", normalFont));
        if (company.getTaxId() != null) {
            buyer.add(new Chunk("PIB: " + company.getTaxId() + "\n", normalFont));
        }
        if (company.getRegistrationNumber() != null) {
            buyer.add(new Chunk("Matični broj: " + company.getRegistrationNumber() + "\n", normalFont));
        }
        
        StringBuilder address = new StringBuilder();
        if (company.getStreet() != null) {
            address.append(company.getStreet());
            if (company.getStreetNumber() != null) {
                address.append(" ").append(company.getStreetNumber());
            }
        }
        if (company.getPostalCode() != null) {
            address.append("\n").append(company.getPostalCode());
        }
        if (company.getCity() != null) {
            address.append(" ").append(company.getCity().getName());
        }
        if (company.getCountry() != null) {
            address.append(", ").append(company.getCountry().getName());
        }
        buyer.add(new Chunk("Adresa: " + address + "\n", normalFont));
        buyer.add(new Chunk("Kontakt: " + customer.getName() + " " + customer.getSurname() + "\n", normalFont));
        buyer.add(new Chunk("Email: " + customer.getUsername() + "\n", normalFont));
        
        buyerCell.addElement(buyer);
        table.addCell(buyerCell);
        
        document.add(table);
    }

    private void addItemsTable(Document document, Order order) throws DocumentException {
        document.add(new Paragraph("\n"));
        
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 4f, 1.5f, 2f, 2f});
        
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        
        // Header row
        String[] headers = {"R.br.", "Naziv proizvoda", "Količina", "Jed. cena", "Ukupno"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(52, 58, 64));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8f);
            table.addCell(cell);
        }
        
        // Data rows
        int rowNum = 1;
        boolean alternate = false;
        for (OrderItem item : order.getItems()) {
            Color bgColor = alternate ? new Color(248, 249, 250) : Color.WHITE;
            
            // Row number
            PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(rowNum++), cellFont));
            numCell.setBackgroundColor(bgColor);
            numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            numCell.setPadding(6f);
            table.addCell(numCell);
            
            // Product name
            String productInfo = item.getProductName() + "\n(SKU: " + item.getProductSku() + ")";
            PdfPCell nameCell = new PdfPCell(new Phrase(productInfo, cellFont));
            nameCell.setBackgroundColor(bgColor);
            nameCell.setPadding(6f);
            table.addCell(nameCell);
            
            // Quantity
            PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), cellFont));
            qtyCell.setBackgroundColor(bgColor);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setPadding(6f);
            table.addCell(qtyCell);
            
            // Unit price
            PdfPCell priceCell = new PdfPCell(new Phrase(formatCurrency(item.getUnitPrice()), cellFont));
            priceCell.setBackgroundColor(bgColor);
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            priceCell.setPadding(6f);
            table.addCell(priceCell);
            
            // Line total
            PdfPCell totalCell = new PdfPCell(new Phrase(formatCurrency(item.getLineTotal()), cellFont));
            totalCell.setBackgroundColor(bgColor);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setPadding(6f);
            table.addCell(totalCell);
            
            alternate = !alternate;
        }
        
        document.add(table);
    }

    private void addTotals(Document document, Order order) throws DocumentException {
        document.add(new Paragraph("\n"));
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{2f, 2f});
        
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font totalFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        
        // Subtotal
        addTotalRow(table, "Međuzbir:", formatCurrency(order.getSubtotal()), labelFont, valueFont);
        
        // Tax
        addTotalRow(table, "PDV (20%):", formatCurrency(order.getTaxAmount()), labelFont, valueFont);
        
        // Total
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("UKUPNO:", totalFont));
        totalLabelCell.setBorder(Rectangle.TOP);
        totalLabelCell.setBorderWidth(2f);
        totalLabelCell.setPadding(8f);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabelCell);
        
        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(order.getTotalAmount()) + " RSD", totalFont));
        totalValueCell.setBorder(Rectangle.TOP);
        totalValueCell.setBorderWidth(2f);
        totalValueCell.setPadding(8f);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalValueCell);
        
        document.add(table);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value + " RSD", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addFooter(Document document, Order order) throws DocumentException {
        document.add(new Paragraph("\n\n"));
        
        Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
        
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            footer.add(new Chunk("Napomena: " + order.getNotes() + "\n\n", footerFont));
        }
        
        footer.add(new Chunk("Hvala Vam na poverenju!\n", footerFont));
        footer.add(new Chunk("Faktura je kreirana elektronski i važeća je bez pečata i potpisa.\n", footerFont));
        footer.add(new Chunk("Datum i vreme generisanja: " + 
                order.getInvoiceGeneratedAt().format(DATETIME_FORMATTER), footerFont));
        
        document.add(footer);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00";
        return String.format("%,.2f", amount).replace(",", " ").replace(".", ",").replace(" ", ".");
    }
}
