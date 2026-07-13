import type {
  PublicGalleryItemViewModel,
  PublicServiceViewModel,
  PublicSiteViewModel,
} from "../types/view-model";
import { getPrimaryGalleryImage } from "../utils/content";
import { AboutSection } from "./AboutSection";
import { ContactSection } from "./ContactSection";
import { GallerySection } from "./GallerySection";
import { HeroSection } from "./HeroSection";
import { MobileWhatsAppAction } from "./MobileWhatsAppAction";
import { ProcessSection } from "./ProcessSection";
import { ServiceAreaSection } from "./ServiceAreaSection";
import { ServiceSection } from "./ServiceSection";
import { TrustStrip } from "./TrustStrip";

type PublicLandingPageProps = {
  site: PublicSiteViewModel;
  services: PublicServiceViewModel[];
  gallery: PublicGalleryItemViewModel[];
  servicesError?: boolean;
  galleryError?: boolean;
};

export function PublicLandingPage({
  site,
  services,
  gallery,
  servicesError = false,
  galleryError = false,
}: PublicLandingPageProps) {
  const primaryImage = getPrimaryGalleryImage(gallery);

  return (
    <>
      <HeroSection site={site} heroImage={primaryImage} />
      <TrustStrip
        hasWhatsApp={Boolean(site.whatsapp)}
        hasPhone={Boolean(site.publicPhone)}
      />
      <ServiceSection
        services={services}
        whatsapp={site.whatsapp}
        isPartialError={servicesError}
      />
      <GallerySection gallery={gallery} isPartialError={galleryError} />
      <ProcessSection />
      <AboutSection site={site} image={primaryImage} />
      <ServiceAreaSection site={site} />
      <ContactSection site={site} />
      <MobileWhatsAppAction site={site} />
    </>
  );
}
